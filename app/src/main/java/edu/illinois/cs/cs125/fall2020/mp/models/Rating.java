package edu.illinois.cs.cs125.fall2020.mp.models;

/** Rating class for storing client rating. */
public class Rating {
  /** Rating indicating the course has not been rated yet. */
  public static final double NOT_RATED = -1.0;

  private String id;

  private double rating;

  /** default constructor. */
  public Rating() {}

  /**
   * constructor for storing clientID and default rating.
   *
   * @param clientID ID associated with the rating
   */
  public Rating(final String clientID) {
    rating = NOT_RATED;
    id = clientID;
  }

  /**
   * Constructor to set ID.
   *
   * @param setId client identification UUID
   * @param setRating rating for the client
   */
  public Rating(final String setId, final double setRating) {
    id = setId;
    rating = setRating;
  }

  /**
   * Get ID from student took the rating.
   *
   * @return UUID of the client
   */
  public String getId() {
    return id;
  }

  /**
   * Get Rating.
   *
   * @return rating from the client
   */
  public double getRating() {
    return rating;
  }
}
