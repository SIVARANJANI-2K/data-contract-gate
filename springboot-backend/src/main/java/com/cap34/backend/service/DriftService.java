package com.cap34.backend.service;

import com.cap34.backend.dto.DriftRequest;
import com.cap34.backend.entity.*;
import com.cap34.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DriftService {
    private final ValidationRunRepository runs;
    private final DriftDetailRepository drifts;

    public DriftService(ValidationRunRepository r, DriftDetailRepository d) {
        runs = r;
        drifts = d;
    }

    public DriftDetail create(DriftRequest r) {
        DriftDetail d = new DriftDetail();
        d.setValidationRun(runs.findById(r.validationId())
                .orElseThrow(() -> new NoSuchElementException("Validation run not found")));
        d.setFieldName(r.fieldName());
        d.setChangeType(r.changeType());
        d.setOldValue(r.oldValue());
        d.setNewValue(r.newValue());
        d.setSeverity(r.severity());
        d.setReason(r.reason());
        return drifts.save(d);
    }

    public List<DriftDetail> byValidation(Long id) {
        return drifts.findByValidationRunValidationId(id);
    }
}
