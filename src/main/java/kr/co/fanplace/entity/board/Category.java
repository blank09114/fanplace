package kr.co.fanplace.entity.board;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "category_tbl",
    uniqueConstraints = @UniqueConstraint(name = "uq_board_category", columnNames = {"board_id", "category_name"})
)
public class Category
{
    @Id
    @Column(name = "category_id", length = 40, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "category_name", length = 20, nullable = false)
    private String name;
}