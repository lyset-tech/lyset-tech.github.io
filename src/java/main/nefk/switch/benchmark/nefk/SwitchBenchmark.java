import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class SwitchBenchmark {
  @Param({"1", "2", "3", "4", "5", "6", "7", "8"})
  int n;

  @Benchmark
  public long lookupSwitch() {
    return Switch.lookupSwitch(n);
  }

  @Benchmark
  public long tableSwitch() {
    return Switch.tableSwitch(n);
  }
}
