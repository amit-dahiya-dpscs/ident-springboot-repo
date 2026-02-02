package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.ExpungementRequest;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpungementService {

    private final IdentMasterRepository masterRepo;
    private final IdentNameRepository nameRepo;
    private final IdentDocumentRepository docRepo;
    private final IdentAddressRepository addressRepo;
    private final IdentSSNRepository ssnRepo;
    private final IdentScarsMarksRepository scarsRepo;
    private final IdentDLRepository dlRepo;
    private final IdentMiscNumRepository miscNumRepo;
    private final IdentHenryFPRepository henryFpRepo;
    private final IdentNcicFPRepository ncicFpRepo;
    private final IdentDobAliasRepository dobAliasRepo;

    private final IdentExpungementRepository expungementRepo;
    private final IdentFbiDowngradeRepository fbiDowngradeRepo;
    private final IdentFbiMasterRepository fbiMasterRepo;

    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    private static final List<String> CRIMINAL_TYPES = List.of(
            "BIN", "DOC", "DIO", "IUR", "PAA", "PAB", "COF", "PAR", "PAV", "PAL",
            "WPR", "WPL", "WAR", "WAA", "DET", "CSO", "SOR", "SVO", "SVP", "OFF",
            "CIE", "PAC", "PAD", "PAE", "PAF", "PAG", "PAH", "PAI", "PAJ",
            "209", "211", "CAR", "CNS", "DPP"
    );

    @Transactional
    public String processExpungement(ExpungementRequest req) {
        IdentMaster master = masterRepo.findById(req.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("System ID not found: " + req.getSystemId()));

        long crimCount = docRepo.countByMaster_SystemIdAndDocumentTypeIn(master.getSystemId(), CRIMINAL_TYPES);
        long totalCount = docRepo.countByMaster_SystemId(master.getSystemId());
        long nonCrimCount = totalCount - crimCount;

        switch (req.getDeleteType().toUpperCase()) {
            case "PART_CANCEL":
                processPartCancel(master, req, crimCount);
                return null; // No warning for Part Cancel
            case "DOWNGRADE":
                processDowngrade(master, req, crimCount, nonCrimCount);
                return null;
            case "CANCEL_ENTIRE":
                return processCancelEntire(master, req, crimCount, nonCrimCount);
            case "PARTIAL":
                processPartial(req);
                return null;
            case "CANCEL":
                processCancel(req);
                return null;
            default:
                throw new IllegalArgumentException("Invalid Delete Type: " + req.getDeleteType());
        }
    }

    private String processCancelEntire(IdentMaster master, ExpungementRequest req, long crimCount, long nonCrimCount) {
        // VALIDATION: Ensure strictly ONE criminal event and ZERO non-criminal events exist.
        if (crimCount > 1) {
            throw new IllegalStateException("MULTIPLE ARREST EVENTS EXIST – CAN NOT PERFORM AN EXPUNGE ENTIRE.");
        }

        // FIX: This check now blocks if there are ANY non-criminal records (e.g. 1 CAR + 1 MAF),
        // or if there are only non-criminal records (0 Criminal).
        if (nonCrimCount > 0 || crimCount == 0) {
            throw new IllegalStateException("NON-CRIMINAL EVENTS EXIST - CAN NOT PERFORM AN EXPUNGE ENTIRE ON THIS RECORD!");
        }

        // Get Latest Criminal Date (LocalDate)
        LocalDate eventDate = getLatestCriminalDate(master.getSystemId());
        String warningMessage = null;

        // FBI OWNERSHIP CHECK
        if (fbiMasterRepo.existsBySid(master.getSid())) {
            // 1. Log to FBI Downgrade table (Modern Requirement)
            createFbiDowngradeLog(master, req, eventDate);

            // 2. Log to Standard Expungement table with Indicator 'X' (Legacy Parity with II0500C)
            // This ensures the record exists in the main log but marks it as FBI-owned ('X') so IP07 skips DRS.
            createStandardExpungementLog(master, req, "EXP", "X", eventDate);

            // 3. Set Warning Message
            warningMessage = "REC IS FBI OWNED - DRS MSG NOT SENT";

            // NOTE: We do NOT call triggerIp07Transaction() here, effectively suppressing the DRS message.
        } else {
            // NON-FBI OWNED Logic
            if (master.getFbiNumber() == null || master.getFbiNumber().trim().isEmpty()) {
                // Check if subject is on III (Status S or M)
                if ("S".equalsIgnoreCase(master.getIiiStatus()) || "M".equalsIgnoreCase(master.getIiiStatus())) {
                    throw new IllegalStateException("GIVE THIS CASE TO YOUR SUPERVISOR. NO DRS MSG WAS SENT – THE FBI # IS MISSING.");
                }
            }
            // Log with 'E' for Cancel Entire
            createStandardExpungementLog(master, req, "EXP", "E", eventDate);

            // Trigger downstream (IPS/IP07) processing
            triggerIp07Transaction(master.getSid());
        }

        // Perform the deletion
        deleteAllChildRecords(master.getSystemId());
        masterRepo.delete(master);

        auditService.logAction(req.getUsername(), req.getUserIp(), "CANCEL_ENTIRE", "Deleted Entire SID: " + master.getSid());

        return warningMessage;
    }

    private void processDowngrade(IdentMaster master, ExpungementRequest req, long crimCount, long nonCrimCount) {
        if (crimCount == 0) throw new IllegalStateException("MUST HAVE AN EXISTING CRIMINAL EVENT TO PERFORM THIS DOWNGRADE FUNCTION.");
        if (crimCount > 1) throw new IllegalStateException("MULTIPLE ARREST EVENTS EXIST – CAN NOT PERFORM A DOWNGRADE. USE PART CANCEL.");
        if (nonCrimCount == 0) throw new IllegalStateException("MUST HAVE AN EXISTING NON-CRIMINAL EVENT TO PERFORM THIS DOWNGRADE FUNCTION.");

        LocalDate eventDate = getLatestCriminalDate(master.getSystemId());

        boolean isRapback = "R".equalsIgnoreCase(master.getRapbackSubscriptionIndicator());
        master.setRecordType("N");
        if (!isRapback) master.setFbiNumber(null);
        masterRepo.save(master);

        docRepo.findByMaster_SystemId(master.getSystemId()).stream()
                .filter(d -> CRIMINAL_TYPES.contains(d.getDocumentType()))
                .forEach(docRepo::delete);

        createStandardExpungementLog(master, req, "DWN", "D", eventDate);
        triggerIp07Transaction(master.getSid());
        auditService.logAction(req.getUsername(), req.getUserIp(), "DOWNGRADE", "Downgraded SID: " + master.getSid());
    }

    private void processPartCancel(IdentMaster master, ExpungementRequest req, long crimCount) {
        if (req.getDocumentId() == null) throw new IllegalArgumentException("Document ID required.");

        IdentDocument doc = docRepo.findById(req.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + req.getDocumentId()));

        LocalDate eventDate = doc.getDocumentDate(); // Direct LocalDate assignment

        boolean isCriminalDoc = CRIMINAL_TYPES.contains(doc.getDocumentType());
        if (isCriminalDoc && crimCount <= 1) {
            throw new IllegalStateException("Cannot delete the last criminal event via Part Cancel. Use 'Downgrade'.");
        }

        docRepo.delete(doc);

        createStandardExpungementLog(master, req, "EXP", "C", eventDate);
        triggerIp07Transaction(master.getSid());
        auditService.logAction(req.getUsername(), req.getUserIp(), "PART_CANCEL", "Deleted Doc ID: " + req.getDocumentId());
    }

    private void processPartial(ExpungementRequest req) {
        if (req.getDocumentId() == null) throw new IllegalArgumentException("Document ID required.");

        // 1. Fetch the document BEFORE deleting it to get the Event Date
        IdentDocument doc = docRepo.findById(req.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + req.getDocumentId()));

        LocalDate eventDate = doc.getDocumentDate();

        // 2. Fetch Master record (needed for the log method)
        IdentMaster master = doc.getMaster();

        // 3. Delete the record
        docRepo.delete(doc);

        // 4. Log to T_IDENT_EXPUNGEMENT (IPT_RWEXP)
        // Using 'EXP' for Process Type and 'P' for Indicator
        createStandardExpungementLog(master, req, "EXP", "P", eventDate);

        // 5. Audit
        auditService.logAction(req.getUsername(), req.getUserIp(), "PARTIAL", "Partial expungement Doc ID: " + req.getDocumentId());
    }

    private void processCancel(ExpungementRequest req) {
        if (req.getDocumentId() == null) throw new IllegalArgumentException("Document ID required.");
        docRepo.deleteById(req.getDocumentId());
        auditService.logAction(req.getUsername(), req.getUserIp(), "CANCEL", "Cancelled Doc ID: " + req.getDocumentId());
    }

    // --- Helpers ---

    private void createStandardExpungementLog(IdentMaster master, ExpungementRequest req, String processType, String fbiInd, LocalDate eventDate) {
        IdentExpungement log = new IdentExpungement();
        log.setSid(master.getSid());
        log.setSystemId(master.getSystemId());
        log.setProcessType(processType);
        log.setFbiExpIndicator(fbiInd);
        log.setUserId(req.getUsername());
        log.setFbiNumber(master.getFbiNumber());
        log.setEventDate(eventDate); // Passed directly as LocalDate

        IdentName pName = getPrimaryName(master.getSystemId());
        log.setLastName(pName.getLastName());
        log.setFirstName(pName.getFirstName());
        log.setMiddleName(pName.getMiddleName());
        log.setDob(pName.getDateOfBirth() != null ? pName.getDateOfBirth().toString() : null);

        log.setRaceCode(master.getRaceCode());
        log.setSexCode(master.getSexCode());
        log.setHeight(master.getHeight());
        log.setWeight(master.getWeight());
        log.setHairCode(master.getHairColorCode());
        log.setEyeCode(master.getEyeColorCode());
        log.setSkinToneCode(master.getSkinToneCode());
        log.setPobCode(master.getPlaceOfBirthCode());

        Optional<IdentAddress> addr = addressRepo.findByMaster_SystemId(master.getSystemId()).stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCurrent())).findFirst();
        addr.ifPresent(a -> {
            log.setStreetNumber(a.getStreetNumber());
            log.setStreetName(a.getStreetName());
            log.setCityName(a.getCity());
            log.setStateCode(a.getStateCode());
            log.setZipCode(a.getZipCode());
        });

        ssnRepo.findByMaster_SystemId(master.getSystemId()).stream().findFirst()
                .ifPresent(s -> log.setSsn(s.getSsn()));

        log.setCogentPcn(req.getCogentPcn());
        log.setCogentPcn2(req.getCogentPcn2());
        log.setCourtCaseNumber(req.getCourtCaseNumber());
        log.setChargeDescription(req.getCharge());
        log.setReasonForDeletion(req.getReason());

        expungementRepo.save(log);
    }

    private void createFbiDowngradeLog(IdentMaster master, ExpungementRequest req, LocalDate eventDate) {
        IdentFbiDowngrade log = new IdentFbiDowngrade();
        log.setSid(master.getSid());
        log.setFbiNumber(master.getFbiNumber());
        log.setSystemId(master.getSystemId());
        log.setUserId(req.getUsername());
        log.setFbiRecordIndicator("Y");
        log.setArrestDate(eventDate); // Passed directly as LocalDate

        IdentName pName = getPrimaryName(master.getSystemId());
        log.setLastName(pName.getLastName());
        log.setFirstName(pName.getFirstName());
        log.setMiddleName(pName.getMiddleName());
        log.setDob(pName.getDateOfBirth() != null ? pName.getDateOfBirth().toString() : null);

        ssnRepo.findByMaster_SystemId(master.getSystemId()).stream().findFirst()
                .ifPresent(s -> log.setSsn(s.getSsn()));

        log.setPcn(req.getCogentPcn());
        log.setCourtCase(req.getCourtCaseNumber());
        log.setChargeDescription(req.getCharge());

        fbiDowngradeRepo.save(log);
    }

    private IdentName getPrimaryName(Long sysId) {
        List<IdentName> names = nameRepo.findByMaster_SystemId(sysId);
        return names.stream()
                .filter(n -> "P".equals(n.getNameType()))
                .findFirst()
                .orElseGet(() -> names.isEmpty() ? new IdentName() : names.get(0));
    }

    private LocalDate getLatestCriminalDate(Long sysId) {
        return docRepo.findByMaster_SystemId(sysId).stream()
                .filter(d -> CRIMINAL_TYPES.contains(d.getDocumentType()))
                .max(Comparator.comparing(IdentDocument::getDocumentDate))
                .map(IdentDocument::getDocumentDate)
                .orElse(null);
    }

    private void deleteAllChildRecords(Long sysId) {
        docRepo.deleteAllByMaster_SystemId(sysId);
        nameRepo.deleteAll(nameRepo.findByMaster_SystemId(sysId));
        ssnRepo.deleteAll(ssnRepo.findByMaster_SystemId(sysId));
        addressRepo.deleteAll(addressRepo.findByMaster_SystemId(sysId));
        scarsRepo.deleteAll(scarsRepo.findByMaster_SystemId(sysId));
        dlRepo.deleteAll(dlRepo.findByMaster_SystemId(sysId));
        miscNumRepo.deleteAll(miscNumRepo.findByMaster_SystemId(sysId));
        henryFpRepo.deleteAll(henryFpRepo.findByMaster_SystemId(sysId));
        ncicFpRepo.deleteAll(ncicFpRepo.findByMaster_SystemId(sysId));
        dobAliasRepo.deleteAll(dobAliasRepo.findByMaster_SystemId(sysId));
    }

    private void triggerIp07Transaction(String sid) {
       // eventPublisher.publishEvent(new ExpungementCreatedEvent(this, sid));
    }
}