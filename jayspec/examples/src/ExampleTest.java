import java.util.ArrayList;

import com.github.forax.jayspec.JaySpec;

public interface ExampleTest {
  public static void main(String[] args) {
    new JaySpec() {{
      describe(ArrayList.class, it -> {
        given("an empty list", () -> {
          ArrayList<String> list = new ArrayList<>();
          
          it.should("have a size == 0", verify -> {
            verify.that(list.size()).isEqualTo(0);
          });
        });
        
        given("a list of one element", () -> {
          ArrayList<String> list = new ArrayList<>();
          list.add("hello");
          
          it.should("have a size == 1", verify -> {
            verify.that(list.size()).isEqualTo(1);
          });
          
          it.should("get the item at index 0", verify -> {
            verify.that(list.get(0)).isEqualTo("hello");
          });
          
          it.should("not return a valid index for a different item", verify -> {
            verify.that(list.indexOf("not hello")).isEqualTo(-1);
          });
        });
      });
    }}.run();
  }

}
