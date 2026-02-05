package kr.co.fanplace.repository;

import java.time.LocalDate;

public interface DayCountRow
{
    LocalDate getDay();
    long getCnt();
}