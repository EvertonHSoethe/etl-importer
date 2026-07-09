package domain;

import enums.ExecutionStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedDate;

    private LocalDateTime finishedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatusEnum status;

    @Column(nullable = false)
    private Long totalLines;

    @Column(nullable = false)
    private Long processedLines;

    @Column(nullable = false)
    private Long successLines;

    @Column(nullable = false)
    private Long errorLines;

    /**
     * Duração em milissegundos.
     */
    private Long duration;
}