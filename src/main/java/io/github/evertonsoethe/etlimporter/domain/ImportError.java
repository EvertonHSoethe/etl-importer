package io.github.evertonsoethe.etlimporter.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_error")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private ImportExecution execution;

    @Column(nullable = false)
    private Long lineNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String errorMessage;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawData;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}