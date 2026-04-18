package com.library.app.repository;

import com.library.app.domain.BorrowRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    List<BorrowRequest> findByBookId(Long bookId);
    List<BorrowRequest> findByBookIdAndStatus(Long bookId, String status);
    List<BorrowRequest> findByUserId(Long userId);

    long countByUserIdAndStatusIn(Long userId, List<String> statuses);

    // Đếm số PENDING để hiển thị badge cảnh báo
    long countByStatus(String status);

    // Stats charts
    @Query("SELECT b.borrowDate, COUNT(b) FROM BorrowRequest b WHERE b.borrowDate IS NOT NULL GROUP BY b.borrowDate ORDER BY b.borrowDate ASC")
    List<Object[]> countBorrowsByDate();

    @Query("SELECT b.returnDate, COUNT(b) FROM BorrowRequest b WHERE b.status = 'RETURNED' AND b.returnDate IS NOT NULL GROUP BY b.returnDate ORDER BY b.returnDate ASC")
    List<Object[]> countReturnsByDate();

    // Due-date reminders / overdue
    List<BorrowRequest> findByStatusAndDueDate(String status, java.time.LocalDate dueDate);
    List<BorrowRequest> findByStatusAndDueDateBefore(String status, java.time.LocalDate date);

    /**
     * Truy vấn ưu tiên: PENDING lên đầu, sau đó OVERDUE, BORROWING, RETURNED, REJECTED.
     * Trong cùng nhóm, sort theo createdAt DESC (mới nhất trước).
     * Hỗ trợ filter theo status và keyword (tên người mượn / tên sách).
     */
    @Query("""
        SELECT r FROM BorrowRequest r
        WHERE (:status IS NULL OR :status = '' OR r.status = :status)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.book.title)    LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY
            CASE r.status
                WHEN 'PENDING'   THEN 0
                WHEN 'OVERDUE'   THEN 1
                WHEN 'BORROWING' THEN 2
                WHEN 'RETURNED'  THEN 3
                WHEN 'REJECTED'  THEN 4
                ELSE 5
            END ASC,
            r.createdAt DESC
        """)
    Page<BorrowRequest> findAllWithPriorityAndFilter(
        @Param("status")  String status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}