package io.repository;

import io.model.entity.Location;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
@Transactional
public class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SessionFactory sessionFactory;

    private final Long userId1 = 1L;
    private final Long userId2 = 2L;

    @Test
    void saveAndFindAllByUserId_ShouldReturnSavedLocationsForUser() {

        Location berlin = new Location("Berlin", userId1, 52.52, 13.40);
        Location paris = new Location("Paris", userId1, 48.85, 2.35);
        Location tokyo = new Location("Tokyo", userId2, 35.68, 139.69);

        locationRepository.save(berlin);
        locationRepository.save(paris);
        locationRepository.save(tokyo);

        List<Location> user1Locations = locationRepository.findAllByUserId(userId1);

        assertEquals(2, user1Locations.size());
        assertTrue(user1Locations.stream().anyMatch(l -> l.getName().equals("Berlin")));
        assertTrue(user1Locations.stream().anyMatch(l -> l.getName().equals("Paris")));
    }

    @Test
    void countByUser_ShouldReturnCorrectCount() {

        locationRepository.save(new Location("City 1", userId1, 1.0, 1.0));
        locationRepository.save(new Location("City 2", userId1, 2.0, 2.0));
        locationRepository.save(new Location("City 3", userId1, 3.0, 3.0));

        int count = locationRepository.countByUser(userId1);

        assertEquals(3, count);

    }

    @Test
    void existsByUserAndCoords_ShouldReturnTrueIfLocationExists() {

        Location location = new Location("Madrid", userId1, 40.41, -3.70);
        locationRepository.save(location);

        assertTrue(locationRepository.existsByUserAndCoords(userId1, 40.41, -3.70));

    }

    @Test
    void existsByUserAndCoords_ShouldReturnFalseIfLocationDoesNotExist() {
        assertFalse(locationRepository.existsByUserAndCoords(userId1, 0.0, 0.0));
    }

    @Test
    void existsByUserAndCoords_ShouldReturnFalseForDifferentUser() {

        Location location = new Location("Rome", userId1, 41.90, 12.49);
        locationRepository.save(location);

        assertFalse(locationRepository.existsByUserAndCoords(userId2, 41.90, 12.49));

    }

    @Test
    void deleteLocationByUserId_ShouldDeleteCorrectLocation() {

        Location locationToDelete = new Location("London", userId1, 51.50, -0.12);
        locationRepository.save(locationToDelete);
        assertNotNull(locationToDelete.getId());

        int updatedRows = locationRepository.deleteLocationByUserId(userId1, locationToDelete.getId());

        assertEquals(1, updatedRows);

        sessionFactory.getCurrentSession().clear();

        Location deleted = sessionFactory.getCurrentSession().get(Location.class, locationToDelete.getId());
        assertNull(deleted);

    }

    @Test
    void deleteLocationByUserId_ShouldNotDeleteIfUserIdIsWrong() {

        Location location = new Location("Cairo", userId1, 30.04, 31.23);
        locationRepository.save(location);
        assertNotNull(location.getId());

        int updatedRows = locationRepository.deleteLocationByUserId(userId2, location.getId());

        assertEquals(0, updatedRows);

        Location notDeleted = sessionFactory.getCurrentSession().get(Location.class, location.getId());
        assertNotNull(notDeleted);

    }
}