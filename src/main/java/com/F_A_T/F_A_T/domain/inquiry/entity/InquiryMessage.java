package com.F_A_T.F_A_T.domain.inquiry.entity;

import com.F_A_T.F_A_T.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_messages")
@Getter
@NoArgsConstructor
public class InquiryMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message_content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public InquiryMessage(Inquiry inquiry, User sender, String content) {
        this.inquiry = inquiry;
        this.sender = sender;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }
}
