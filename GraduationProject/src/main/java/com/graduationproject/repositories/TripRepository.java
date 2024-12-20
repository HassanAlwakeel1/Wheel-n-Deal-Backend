package com.graduationproject.repositories;

import com.graduationproject.entities.Trip;
import com.graduationproject.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip,Integer> {
    List<Trip> findByFromAndTo(String from, String to);
    List<Trip> findByUser(User commuter);
}