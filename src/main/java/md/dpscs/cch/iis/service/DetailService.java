package md.dpscs.cch.iis.service;

import md.dpscs.cch.iis.dto.*;
import md.dpscs.cch.iis.model.*;
import md.dpscs.cch.iis.repository.*;
import md.dpscs.cch.iis.util.MainframeDataUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetailService {

    // Inject all 11 Repositories representing the 13 data sources
    private final PersonRepository personRepository;
    private final PersonNameRepository personNameRepository;
    private final PersonFlagRepository personFlagRepository;
    private final PersonAttributeRepository personAttributeRepository;
    private final PersonIDSecondaryRepository personIdSecondaryRepository;
    private final PersonAltDOBRepository personAltDOBRepository;
    private final DocumentArrestRepository documentArrestRepository;
    private final DocumentIndexRepository documentIndexRepository;
    private final DocumentGeneralRefRepository documentGeneralRefRepository;

    private final MainframeDataUtils mainframeDataUtils;

    // Custom Exception for clean API responses
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Aggregates data from 13 tables to build the complete Detail View (II0400C logic).
     * This replaces the multi-cursor logic (0050-SELECT-IDENT through 1010-FETCH-AREST).
     */
    @Transactional(readOnly = true)
    public PersonDetailDTO getPersonDetails(Long personId) {

        // 1. CORE DATA FETCH (0050-SELECT-IDENT logic)
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person ID not found: " + personId));

        PersonDetailDTO dto = new PersonDetailDTO();

        // Map core fields (from Person entity)
        dto.setPersonId(person.getPersonId());
        dto.setStateId(person.getStateId());
        dto.setFbiNumber(person.getFbiNumber());
        dto.setRecordType(person.getRecordType());
        dto.setComments(person.getComments());
        dto.setMugshotFlag(person.getMugshotFlag());
        dto.setPrimaryDateOfBirth(person.getDateOfBirth());

        // 2. AGGREGATION (Mirroring 13 Cursors/Fetches in II0400C)

        // A. Alias Section (II0400C FETCH ALL NAMESROW logic)
        dto.setNamesAndAliases(personNameRepository.findAllById_PersonId(personId).stream()
                .map(this::convertToNameDTO)
                .collect(Collectors.toList()));

        // B. Appended-ID Section (II0400C 0150-INTEGRATE-APPEN-INFO logic)
        dto.setFlags(personFlagRepository.findByPersonId(personId).stream()
                .map(this::convertToFlagDTO)
                .collect(Collectors.toList()));

        dto.setCautionsAndScars(personAttributeRepository.findAllByPersonId(personId).stream()
                .map(this::convertToAttributeDTO)
                .collect(Collectors.toList()));

        dto.setSecondaryIdentifiers(personIdSecondaryRepository.findAllByPersonId(personId).stream()
                .map(this::convertToSecondaryIDDTO)
                .collect(Collectors.toList()));

        dto.setAlternateDOBs(personAltDOBRepository.findAllByPersonId(personId).stream()
                .map(this::convertToAltDOBDTO)
                .collect(Collectors.toList()));

        // D. Document Section (II0400C 0800, 0900, 1000 Logic)
        dto.setArrestDocuments(documentArrestRepository.findAllByPersonIdOrderByDocumentDateDesc(personId).stream().map(this::convertToDocumentDTO).collect(Collectors.toList()));

        dto.setIndexDocuments(documentIndexRepository.findAllByPersonIdOrderByDocumentDateDesc(personId).stream()
                .map(this::convertToDocumentDTO)
                .collect(Collectors.toList()));

        // General References (Must use explicit lambda to match DocumentGeneralRef Entity)
        dto.setGeneralReferences(documentGeneralRefRepository.findAllByPersonIdOrderByDocumentDateDesc(personId).stream()
                .map(this::convertToDocumentDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    // --- Private Conversion Methods (Entity to DTO Mapping) ---

    private NameDTO convertToNameDTO(PersonName entity) {
        NameDTO dto = new NameDTO();
        dto.setLastName(entity.getLastName());
        dto.setFirstName(entity.getFirstName());
        dto.setMiddleInit(entity.getMiddleInit());
        dto.setRestMiddleName(entity.getRestMiddleName());
        dto.setBirthDate(entity.getBirthDate());
        dto.setFingerprintMafis(entity.getFingerprintMafis());
        return dto;
    }

    private FlagDTO convertToFlagDTO(PersonFlag entity) {
        FlagDTO dto = new FlagDTO();
        dto.setFlagName(entity.getFlagName());
        dto.setFlagValue(entity.getFlagValue());
        dto.setLastUpdateDate(entity.getLastUpdateDate());
        return dto;
    }

    private AttributeDTO convertToAttributeDTO(PersonAttribute entity) {
        AttributeDTO dto = new AttributeDTO();
        dto.setAttributeType(entity.getAttributeType());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private SecondaryIDDTO convertToSecondaryIDDTO(PersonIDSecondary entity) {
        SecondaryIDDTO dto = new SecondaryIDDTO();
        dto.setIdType(entity.getIdType());
        dto.setIdValue(entity.getIdValue());
        dto.setIssuingSource(entity.getIssuingSource());
        return dto;
    }

    private AltDOBDTO convertToAltDOBDTO(PersonAltDOB entity) {
        AltDOBDTO dto = new AltDOBDTO();
        dto.setDob(entity.getDob());
        return dto;
    }
    private DocumentDTO convertToDocumentDTO(DocumentArrest entity) {
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setDocumentDate(entity.getDocumentDate());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private DocumentDTO convertToDocumentDTO(DocumentIndex entity) {
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setDocumentDate(entity.getDocumentDate());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private DocumentDTO convertToDocumentDTO(DocumentGeneralRef entity) {
        DocumentDTO dto = new DocumentDTO();
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentNumber(entity.getDocumentNumber());
        dto.setDocumentDate(entity.getDocumentDate());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}