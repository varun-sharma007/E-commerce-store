package com.ecom.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecom.model.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	List<Product> findByIsActiveTrue();

	Page<Product> findByIsActiveTrue(Pageable pageable);

	List<Product> findByCategory(String category);

	List<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2);

	Page<Product> findByCategory(Pageable pageable, String category);

	Page<Product> findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2,
			Pageable pageable);

	Page<Product> findByisActiveTrueAndTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(String ch, String ch2,
			Pageable pageable);

	// Chatbot queries
	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.discountPrice <= :maxPrice ORDER BY p.discountPrice ASC")
	List<Product> findByIsActiveTrueAndDiscountPriceLessThanEqual(@Param("maxPrice") Double maxPrice);

	@Query("SELECT p FROM Product p WHERE p.isActive = true AND LOWER(p.category) = LOWER(:category) AND p.discountPrice <= :maxPrice ORDER BY p.discountPrice ASC")
	List<Product> findByIsActiveTrueAndCategoryIgnoreCaseAndDiscountPriceLessThanEqual(
			@Param("category") String category, @Param("maxPrice") Double maxPrice);

	@Query("SELECT p FROM Product p WHERE p.isActive = true AND LOWER(p.category) = LOWER(:category) ORDER BY p.discountPrice ASC")
	List<Product> findByIsActiveTrueAndCategoryIgnoreCase(@Param("category") String category);

	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0 ORDER BY p.discountPrice ASC")
	List<Product> findByIsActiveTrueAndStockGreaterThan();

	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0 AND p.discountPrice <= :maxPrice ORDER BY p.discountPrice ASC")
	List<Product> findInStockUnderPrice(@Param("maxPrice") Double maxPrice);

	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stock > 0 AND LOWER(p.category) = LOWER(:category) AND p.discountPrice <= :maxPrice ORDER BY p.discountPrice ASC")
	List<Product> findInStockByCategoryUnderPrice(@Param("category") String category, @Param("maxPrice") Double maxPrice);
}

