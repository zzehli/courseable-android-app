package edu.illinois.cs.cs125.fall2020.mp.models;

/**
 * Model holding more detailed course information shown in the individual course view.
 *
 * <p>MP1 implementation task.
 */
public class Course extends Summary {
  private String description;

  /**
   * Get the description for this course.
   *
   * @return the course description as a String
   */
  public final String getDescription() {
    return description;
  }
  /** Default constructor. */
  public Course() {}
  /**
   * Create a Summary with the provided fields.
   *
   * @param setYear the year for this Course
   * @param setSemester the semester for this Course
   * @param setDepartment the department for this Course
   * @param setNumber the number for this Course
   * @param setTitle the title for this Course
   * @param setDescription the description for the Course
   */
  public Course(
      final String setYear,
      final String setSemester,
      final String setDepartment,
      final String setNumber,
      final String setTitle,
      final String setDescription) {
    super(setYear, setSemester, setDepartment, setNumber, setTitle);
    description = setDescription;
  }
}
