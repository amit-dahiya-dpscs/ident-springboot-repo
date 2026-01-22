package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.ExpungementRequest;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpungementService {

    private final IdentMasterRepository masterRepo;
    private final IdentDocumentRepository docRepo;
    private final IdentNameRepository nameRepo;
    private final IdentExpungementRepository expungementRepo;
    private final AuditService auditService;

    // Criminal Codes defined in Legacy Specs
    private static final List<String> CRIMINAL_TYPES = List.of(
            "BIN", "DOC", "DIO", "IUR", "PAA", "PAB", "COF", "PAR", "PAV", "PAL",
            "WPR", "WPL", "WAR", "WAA", "DET", "CSO", "SOR", "SVO", "SVP", "OFF",
            "CIE", "PAC", "PAD", "PAE", "PAF", "PAG", "PAH", "PAI", "PAJ",
            "209", "211", "CAR", "CNS", "DPP"
    );

    @Transactional
    public void processExpungement(ExpungementRequest req) {
        IdentMaster master = masterRepo.findById(req.getSystemId())
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        long crimCount = docRepo.countCriminalRecords(master.getSystemId(), CRIMINAL_TYPES);

        // 1. Log Transaction to T_IDENT_EXPUNGEMENT (FRD 678)
        createExpungementLog(master, req);

        // 2. Execute Logic based on Delete Type
        switch (req.getDeleteType().toUpperCase()) {
            case "PART_CANCEL":
                processPartCancel(req, crimCount);
                break;
            case "DOWNGRADE":
                processDowngrade(master, req, crimCount);
                break;
            case "CANCEL_ENTIRE":
                processCancelEntire(master, req, crimCount);
                break;
            case "PARTIAL": // FRD Section 8
                processPartial(req);
                break;
            case "CANCEL":  // FRD Section 7
                processCancel(req);
                break;
            default:
                throw new IllegalArgumentException("Invalid Delete Type: " + req.getDeleteType());
        }
    }

    private void processPartCancel(ExpungementRequest req, long crimCount) {
        validateDocumentId(req);
        IdentDocument doc = docRepo.findById(req.getDocumentId()).orElseThrow();

        // FRD 96 (II0600C): Cannot delete the LAST criminal record via Part Cancel
        boolean isCriminal = CRIMINAL_TYPES.contains(doc.getDocumentType());
        if (isCriminal && crimCount <= 1) {
            throw new IllegalStateException("Cannot delete the last criminal event. Use 'Downgrade' instead.");
        }

        docRepo.delete(doc);

        if (req.getComments() != null) {
            IdentMaster master = masterRepo.findById(req.getSystemId()).orElseThrow();
            master.setComments(req.getComments());
            masterRepo.save(master);
        }
        // FRD 678: Log Action (Fixed 4 argument call)
        auditService.logAction(req.getUsername(), req.getUserIp(), "PART_CANCEL", "Deleted Ref ID: " + req.getDocumentId());
    }

    private void processPartial(ExpungementRequest req) {
        validateDocumentId(req);
        docRepo.deleteById(req.getDocumentId());

        // FRD 659: Do NOT send message to TC for Partial.
        auditService.logAction(req.getUsername(), req.getUserIp(), "PARTIAL", "Partial expungement of Ref ID: " + req.getDocumentId());
    }

    private void processCancel(ExpungementRequest req) {
        validateDocumentId(req);
        docRepo.deleteById(req.getDocumentId());

        // FRD 584: Do NOT send messages to TC, ETS, or FBI for "Cancel".
        auditService.logAction(req.getUsername(), req.getUserIp(), "CANCEL", "Cancelled Ref ID: " + req.getDocumentId());
    }

    private void processDowngrade(IdentMaster master, ExpungementRequest req, long crimCount) {
        // FRD 5.4 check
        if (crimCount == 0) {
            throw new IllegalStateException("MUST HAVE AN EXISTING CRIMINAL EVENT TO PERFORM THIS DOWNGRADE FUNCTION.");
        }

        // FRD 5.6: Rapback Check (Locks UCN)
        boolean isRapback = "R".equalsIgnoreCase(master.getRapbackSubscriptionIndicator()) ||
                "Y".equalsIgnoreCase(master.getRapbackSubscriptionIndicator());

        if (isRapback) {
            // FRD 410: UCN is protected. Ignore any UCN input.
        } else if (req.getUcn() != null) {
            // FRD 407: UCN editable. If user cleared it, set to null.
            master.setFbiNumber(req.getUcn().isEmpty() ? null : req.getUcn());
        }

        if (req.getComments() != null) {
            master.setComments(req.getComments());
        }

        // Delete ALL Criminal Records
        List<IdentDocument> docs = docRepo.findByMaster_SystemId(master.getSystemId());
        docs.stream()
                .filter(d -> CRIMINAL_TYPES.contains(d.getDocumentType()))
                .forEach(docRepo::delete);

        // Update Master Status
        master.setRecordType("N");
        if (!isRapback) {
            master.setFbiNumber(null); // FRD 5.10 (Standard Downgrade removes UCN if not Rapback)
        }

        masterRepo.save(master);
        auditService.logAction(req.getUsername(), req.getUserIp(), "DOWNGRADE", "Downgraded SID: " + master.getSid());
    }

    private void processCancelEntire(IdentMaster master, ExpungementRequest req, long crimCount) {
        // FRD 4.4 check
        if (crimCount > 1) {
            throw new IllegalStateException("MULTIPLE ARREST EVENTS EXIST – CAN NOT PERFORM AN EXPUNGE ENTIRE.");
        }

        // FRD User Story 1: Check III Status for DRS Message [cite: 231-233]
        boolean isOnIII = "S".equalsIgnoreCase(master.getIiiStatus()) ||
                "M".equalsIgnoreCase(master.getIiiStatus());

        if (isOnIII) {
            if (master.getFbiNumber() == null) {
                // FRD 257: Error if III status is set but FBI# is missing
                throw new IllegalStateException("GIVE THIS CASE TO YOUR SUPERVISOR. NO DRS MSG WAS SENT – THE FBI # IS MISSING.");
            }
            // Trigger DRS Async Message here
        }

        nameRepo.deleteAll(nameRepo.findByMaster_SystemId(master.getSystemId()));
        docRepo.deleteAll(docRepo.findByMaster_SystemId(master.getSystemId()));
        masterRepo.delete(master);

        auditService.logAction(req.getUsername(), req.getUserIp(), "CANCEL_ENTIRE", "Deleted Entire SID: " + master.getSid());
    }

    private void createExpungementLog(IdentMaster master, ExpungementRequest req) {
        IdentExpungement log = new IdentExpungement();
        log.setSid(master.getSid());
        log.setUserId(req.getUsername());
        log.setProcessType(mapProcessType(req.getDeleteType()));
        log.setCogentPcn(req.getCogentPcn());
        log.setCogentPcn2(req.getCogentPcn2());
        log.setCourtCaseNumber(req.getCourtCaseNumber());
        log.setChargeDescription(req.getCharge());
        log.setReasonForDeletion(req.getReason());
        log.setFbiNumber(master.getFbiNumber());

        // Snapshot Name & Date
        IdentName primaryName = nameRepo.findByMaster_SystemId(master.getSystemId()).stream()
                .filter(n -> "P".equals(n.getNameType())).findFirst().orElse(new IdentName());
        log.setLastName(primaryName.getLastName());
        log.setFirstName(primaryName.getFirstName());
        log.setEventDate(LocalDateTime.now().toString());

        expungementRepo.save(log);
    }

    private void validateDocumentId(ExpungementRequest req) {
        if (req.getDocumentId() == null) {
            throw new IllegalArgumentException("Document ID is required.");
        }
    }

    private String mapProcessType(String type) {
        if ("DOWNGRADE".equalsIgnoreCase(type)) return "D";
        if ("CANCEL_ENTIRE".equalsIgnoreCase(type)) return "E";
        if ("PART_CANCEL".equalsIgnoreCase(type)) return "C";
        if ("PARTIAL".equalsIgnoreCase(type)) return "P";
        if ("CANCEL".equalsIgnoreCase(type)) return "X";
        return "?";
    }
}