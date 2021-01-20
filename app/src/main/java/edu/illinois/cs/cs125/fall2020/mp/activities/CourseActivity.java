package edu.illinois.cs.cs125.fall2020.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RatingBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.cs.cs125.fall2020.mp.R;
import edu.illinois.cs.cs125.fall2020.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.fall2020.mp.databinding.ActivityCourseBinding;
import edu.illinois.cs.cs125.fall2020.mp.models.Course;
import edu.illinois.cs.cs125.fall2020.mp.models.Rating;
import edu.illinois.cs.cs125.fall2020.mp.models.Summary;
import edu.illinois.cs.cs125.fall2020.mp.network.Client;

/** Course activity opened when click on courses from summary view. */
public final class CourseActivity extends AppCompatActivity
    implements Client.CourseClientCallbacks, RatingBar.OnRatingBarChangeListener {

  private static final String TAG = CourseActivity.class.getSimpleName();
  private Summary clientcourse;
  // Binding to the layout in activity_course.xml
  private ActivityCourseBinding binding;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_course);

    Intent intent = getIntent();
    try {
      // Receive and parse summary info from main activity
      ObjectMapper mapper = new ObjectMapper();
      String fields = intent.getStringExtra("COURSE");
      Course course = mapper.readValue(fields, Course.class);
      clientcourse = course;
      // Retrieve the client from the application and initiate a course request
      CourseableApplication application = (CourseableApplication) getApplication();
      // System.out.println("clientID");

      application.getCourseClient().getCourse(course, this);
      // get UUID and pass to request
      String clientID = application.getClientID();
      // System.out.println(clientID);
      // Get the course rating if there is one, display it
      application.getCourseClient().getRating(course, clientID, this);
      // Update the coruse rating with POST
      binding.rating.setOnRatingBarChangeListener(this);

    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  /**
   * Callback called when the client has retrieved the request course for this component to display.
   * Bind the title and description to the display
   *
   * @param summary the summary that was retrieved/used for search
   * @param course the course returned from the course API client
   */
  @Override
  public void courseResponse(final Summary summary, final Course course) {
    // Bind to the layout in activity_main.xml
    String title = course.getDepartment() + " " + course.getNumber() + ": " + course.getTitle();
    String description = course.getDescription();
    // CS 100: Freshman Orientation
    binding.courseActivity.setText(title);
    binding.courseBody.setText(description);
  }

  @Override
  public void yourRating(final Summary summary, final Rating rating) {
    // test
    binding.rating.setRating((float) rating.getRating());
  }

  @Override
  public void onRatingChanged(
      final RatingBar ratingBar, final float rating, final boolean fromUser) {
    CourseableApplication application = (CourseableApplication) getApplication();
    Rating clientRating = new Rating(application.getClientID(), rating);
    application.getCourseClient().postRating(clientcourse, clientRating, CourseActivity.this);
  }
}
