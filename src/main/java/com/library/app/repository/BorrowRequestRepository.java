package com.library.app.repository;

import com.library.app.domain.BorrowRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    List<BorrowRequest> findByBookId(Long bookId);
    List<BorrowRequest> findByBookIdAndStatus(Long bookId, String status);
    List<BorrowRequest> findByUserId(Long userId);
    
    long countByUserIdAndStatusIn(Long userId, List<String> statuses);
    @Query("SELECT b.borrowDate, COUNT(b) FROM BorrowRequest b WHERE b.borrowDate IS NOT NULL GROUP BY b.borrowDate ORDER BY b.borrowDate ASC")
    List<Object[]> countBorrowsByDate();
    @Query("SELECT b.returnDate, COUNT(b) FROM BorrowRequest b WHERE b.status = 'RETURNED' AND b.returnDate IS NOT NULL GROUP BY b.returnDate ORDER BY b.returnDate ASC")
    List<Object[]> countReturnsByDate();
}