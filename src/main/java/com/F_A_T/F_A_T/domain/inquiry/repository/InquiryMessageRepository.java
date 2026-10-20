package com.F_A_T.F_A_T.domain.inquiry.repository;

import com.F_A_T.F_A_T.domain.inquiry.entity.Inquiry;
import com.F_A_T.F_A_T.domain.inquiry.entity.InquiryMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryMessageRepository extends JpaRepository<InquiryMessage, Long> {
    List<InquiryMessage> findByInquiryOrderBySentAtAscIdAsc(Inquiry inquiry);
}
