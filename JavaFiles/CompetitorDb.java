package com.milky.trackerWeb.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.milky.trackerWeb.model.Competitor;

public interface CompetitorDb extends MongoRepository<Competitor, ObjectId> {

}
