package com.tradevault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.tradevault.entity.enums.NotificationCategory;
import com.tradevault.entity.enums.NotificationStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", length = 30)
    private NotificationCategory category = NotificationCategory.INFO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", length = 30)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    public Notification(Long userId, String title, String message, String categoryStr) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        setType(categoryStr);
    }



    // Backward compatibility for 'type'
    public String getType() {
        return category != null ? category.name() : null;
    }
    public void setType(String type) {
        if (type != null) {
            try {
                this.category = NotificationCategory.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Try titlecase/camelcase or custom
                for (NotificationCategory cat : NotificationCategory.values()) {
                    if (cat.name().equalsIgnoreCase(type)) {
                        this.category = cat;
                        return;
                    }
                }
                this.category = NotificationCategory.INFO;
            }
        }
    }

    // Backward compatibility for 'isRead'
    public Boolean getIsRead() {
        return status == NotificationStatus.READ;
    }
    public void setIsRead(Boolean isRead) {
        this.status = isRead ? NotificationStatus.READ : NotificationStatus.UNREAD;
    }
}
