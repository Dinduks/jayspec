package com.github.forax.jayspec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class JaySpec {
  @FunctionalInterface
  public interface Behavior {
    public void should(String description, Consumer<JayAssertion> assertionConsumer);
  }
  
  @FunctionalInterface
  public interface TestDefinition {
    public void define(Behavior it);
  }
  
  @FunctionalInterface
  public interface Reporter<R> {
    R createReport(Example example, String description, Throwable error);
  }
  
  public static class Spec {
    private final Class<?> declaredClass;
    private final TestDefinition testDefinition;
    
    public Spec(Class<?> declaredClass, TestDefinition testDefinition) {
      this.declaredClass = declaredClass;
      this.testDefinition = testDefinition;
    }
    
    public Class<?> getDeclaredClass() {
      return declaredClass;
    }
    public TestDefinition getTestDefinition() {
      return testDefinition;
    }
    
    @Override
    public String toString() {
      return "spec of " + declaredClass;
    }
  }
  
  public static class Example {
    private final Spec spec;
    private final String description;
    private final Runnable test;
    
    public Example(Spec spec, String description, Runnable test) {
      this.spec = spec;
      this.description = description;
      this.test = test;
    }
    
    public Spec getSpec() {
      return spec;
    }
    public String getDescription() {
      return description;
    }
    public Runnable getTest() {
      return test;
    }
    
    @Override
    public String toString() {
      return "example of " + spec.getDeclaredClass()+ ' ' + description;
    }
  }
  
  public static class Report {
    private final Example example;
    private final String description;
    private final Throwable error;
    
    public Report(Example example, String description, Throwable error) {
      this.example = example;
      this.description = description;
      this.error = error;
    }
    
    public Example getExample() {
      return example;
    }
    public String getDescription() {
      return description;
    }
    public Throwable getError() {
      return error;
    }
    
    @Override
    public String toString() {
      return "report " + description + ' ' + error + " of " + example;
    }
  }
  
  
  private final ArrayList<Spec> specs = new ArrayList<>();
  private final ThreadLocal<Spec> currentSpec = new ThreadLocal<>();
  private final ThreadLocal<List<Example>> currentExampleList = new ThreadLocal<>();
  
  public void describe(Class<?> classToken, TestDefinition testDefinition) {
    specs.add(new Spec(classToken, testDefinition));
  }
  
  public void given(String description, Runnable action) {
    List<Example> exampleList = currentExampleList.get();
    if (exampleList == null) {
      throw new IllegalStateException("given should be called inside a describe block");
    }
    exampleList.add(new Example(currentSpec.get(), description, action));
  }
  
  public List<Spec> getSpecs() {
    return specs;
  }
  
  public <R> List<R> runTest(Reporter<? extends R> reporter) {
    JayAssertion assertion = new JayAssertion();
    ThreadLocal<Example> currentExample = new ThreadLocal<>();
    ThreadLocal<List<R>> currentReportList = new ThreadLocal<>();
    Behavior behavior = (description, consumer) -> {
      Example example = currentExample.get();
      List<R> reportList = currentReportList.get();
      if (example == null || reportList == null) {
        throw new IllegalStateException("should can only be called in a given block");
      }
      
      Throwable error;
      try {
        consumer.accept(assertion);
        error = null;
      } catch(RuntimeException|Error e) {
        error = e;
      }
      reportList.add(reporter.createReport(example, description, error));
    };
    ArrayList<Example> examples = new ArrayList<>();
    currentExampleList.set(examples);
    try {
      specs.forEach(spec -> {
        currentSpec.set(spec);
        spec.getTestDefinition().define(behavior);
      });
    } finally {
      currentSpec.remove();
      currentExampleList.remove();
    }
    
    return examples.parallelStream().flatMap(example -> {
      ArrayList<R> reportList = new ArrayList<R>();
      currentExample.set(example);
      currentReportList.set(reportList);
      try {
        example.getTest().run();
        return reportList.stream();
      } finally {
        currentExample.remove();
        currentReportList.remove();
      }
    }).collect(Collectors.toList());
  }
  
  public void run() {
    Map<Spec, Map<Example, List<Report>>> map = runTest(Report::new).stream().collect(
        Collectors.groupingBy(report -> report.getExample().getSpec(),
            Collectors.groupingBy(Report::getExample)
        ));
    map.forEach((spec, exampleMap) -> {
      System.out.println(spec.getDeclaredClass());
      exampleMap.forEach((example, reports) -> {
        System.out.println("  given " + example.getDescription());
        reports.forEach(report -> {
          System.out.println("    it should " + report.getDescription());
          Throwable error = report.getError();
          if (error != null) {
            error.printStackTrace();
          }
        });
      });
    });
  }
}
