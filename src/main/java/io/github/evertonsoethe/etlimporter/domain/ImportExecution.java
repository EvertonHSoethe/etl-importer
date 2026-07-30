package io.github.evertonsoethe.etlimporter.domain;

import io.github.evertonsoethe.etlimporter.enums.ExecutionStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Representa uma execução de importação de vendas")
public class ImportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da execução", example = "1")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Nome do arquivo importado", example = "sales_100000.csv")
    private String filename;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Schema(description = "Data/hora de início da execução")
    private LocalDateTime startedDate;

    @Schema(description = "Data/hora de término da execução (null se em andamento)")
    private LocalDateTime finishedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Status atual da execução", example = "RUNNING")
    private ExecutionStatusEnum status;

    @Column(nullable = false)
    @Schema(description = "Total de linhas no arquivo", example = "100000")
    private Long totalLines;

    @Column(nullable = false)
    @Schema(description = "Linhas processadas até o momento", example = "45000")
    private Long processedLines;

    @Column(nullable = false)
    @Schema(description = "Linhas importadas com sucesso", example = "44500")
    private Long successLines;

    @Column(nullable = false)
    @Schema(description = "Linhas com erro", example = "500")
    private Long errorLines;

    @Schema(description = "Duração em milissegundos", example = "12345")
    private Long duration;
}
