package com.northwindmutual.quotegen.quote;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Quote} domain objects.
 */
public interface QuoteRepository extends JpaRepository<Quote, Integer> {

	Optional<Quote> findById(Integer id);

	/**
	 * All quotes generated for a given driver, most recent first.
	 * @param driverId the driver's id
	 * @return the driver's quote history
	 */
	List<Quote> findByDriverIdOrderByCreatedAtDesc(Integer driverId);

}
