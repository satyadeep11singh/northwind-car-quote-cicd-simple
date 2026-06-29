package com.northwindmutual.quotegen.driver;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Driver} domain objects. Method names follow Spring
 * Data naming conventions for automatic query derivation.
 */
public interface DriverRepository extends JpaRepository<Driver, Integer> {

	/**
	 * Retrieve {@link Driver}s whose last name starts with the given value.
	 * @param lastName value to search for
	 * @param pageable pagination information
	 * @return a page of matching {@link Driver}s (empty if none found)
	 */
	Page<Driver> findByLastNameStartingWith(String lastName, Pageable pageable);

	Optional<Driver> findById(Integer id);

}
