package kr.co.fanplace.entity.board;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "board_tbl")
public class Board
{
    @Id
    @Column(name = "board_id", length = 40, nullable = false)
    private String id;

    @Column(name = "board_name", length = 20, nullable = false, unique = true)
    private String name;
}