/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mongodb.core.geo;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.mongodb.core.query.Criteria.*;
import static org.springframework.data.mongodb.core.query.Query.*;

import java.util.List;

import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.TestEntities;
import org.springframework.data.mongodb.core.Venue;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.test.util.MongoTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public abstract class AbstractGeoSpatialTests {

	@Configuration
	static class TestConfig extends AbstractMongoClientConfiguration {

		@Override
		protected String getDatabaseName() {
			return "database";
		}

		@Override
		public MongoClient mongoClient() {
			return MongoTestUtils.client();
		}
	}

	@Autowired MongoTemplate template;

	@Before
	public void setUp() {

		template.setWriteConcern(WriteConcern.JOURNALED);

		createIndex();
		addVenues();
	}

	@After
	public void tearDown() {

		dropIndex();
		removeVenues();
	}

	/**
	 * Create the index required to run the tests.
	 */
	protected abstract void createIndex();

	/**
	 * Remove index
	 */
	protected abstract void dropIndex();

	protected void removeVenues() {
		template.dropCollection(Venue.class);
	}

	protected void addVenues() {
		template.insertAll(TestEntities.geolocation().newYork());
	}

	@Test
	public void geoNear() {

		NearQuery geoNear = NearQuery.near(-73, 40, Metrics.KILOMETERS).num(10).maxDistance(150);

		GeoResults<Venue> result = template.geoNear(geoNear, Venue.class);

		assertThat(result.getContent()).isNotEmpty();
		assertThat(result.getAverageDistance().getMetric()).isEqualTo(Metrics.KILOMETERS);
	}

	@Test
	public void withinCenter() {

		Circle circle = new Circle(-73.99171, 40.738868, 0.01);
		List<Venue> venues = template.find(query(where("location").within(circle)), Venue.class);
		assertThat(venues).hasSize(7);
	}

	@Test
	public void withinCenterSphere() {

		Circle circle = new Circle(-73.99171, 40.738868, 0.003712240453784);
		List<Venue> venues = template.find(query(where("location").withinSphere(circle)), Venue.class);
		assertThat(venues).hasSize(11);
	}

	@Test
	public void withinBox() {

		Box box = new Box(new Point(-73.99756, 40.73083), new Point(-73.988135, 40.741404));
		List<Venue> venues = template.find(query(where("location").within(box)), Venue.class);
		assertThat(venues).hasSize(4);
	}

	@Test
	public void withinPolygon() {

		Point first = new Point(-73.99756, 40.73083);
		Point second = new Point(-73.99756, 40.741404);
		Point third = new Point(-73.988135, 40.741404);
		Point fourth = new Point(-73.988135, 40.73083);

		Polygon polygon = new Polygon(first, second, third, fourth);

		List<Venue> venues = template.find(query(where("location").within(polygon)), Venue.class);
		assertThat(venues).hasSize(4);
	}

	@Test
	public void nearSphere() {

		Point point = new Point(-73.99171, 40.738868);
		Query query = query(where("location").nearSphere(point).maxDistance(0.003712240453784));
		List<Venue> venues = template.find(query, Venue.class);
		assertThat(venues).hasSize(11);
	}

	@Test // DATAMONGO-1360
	public void mapsQueryContainedInNearQuery() {

		Query query = query(where("openingDate").lt(LocalDate.now()));
		template.geoNear(NearQuery.near(1.5, 1.7).spherical(true).query(query), Venue.class);
	}
}
