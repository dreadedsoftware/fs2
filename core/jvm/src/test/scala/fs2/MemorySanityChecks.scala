package fs2

import scala.concurrent.ExecutionContext

// Sanity tests - not run as part of unit tests, but these should run forever
// at constant memory.
//
object ResourceTrackerSanityTest extends App {
  val big = Stream.constant(1).flatMap { n =>
    Stream.bracket(Task.delay(()))(_ => Stream.emits(List(1, 2, 3)), _ => Task.delay(()))
  }
  big.run.unsafeRun()
}

object RepeatPullSanityTest extends App {
  def id[A]: Pipe[Pure, A, A] = _ repeatPull { _.receive1 { (h, t) => Pull.output1(h) as t } }
  Stream.constant(1).covary[Task].throughPure(id).run.unsafeRun()
}

object RepeatEvalSanityTest extends App {
  def id[A]: Pipe[Pure, A, A] = {
    def go: Handle[Pure, A] => Pull[Pure, A, Handle[Pure, A]] =
      _.receive1 { case (h, t) => Pull.output1(h) >> go(t) }
    _ pull go
  }
  Stream.repeatEval(Task.delay(1)).throughPure(id).run.unsafeRun()
}

object AppendSanityTest extends App {
  (Stream.constant(1).covary[Task] ++ Stream.empty).pull(_.echo).run.unsafeRun()
}

object DrainOnCompleteSanityTest extends App {
  import ExecutionContext.Implicits.global
  val s = Stream.repeatEval(Task.delay(1)).pull(_.echo).drain ++ Stream.eval_(Task.delay(println("done")))
  (Stream.empty[Task, Unit] merge s).run.unsafeRun()
}

object ConcurrentJoinSanityTest extends App {
  import ExecutionContext.Implicits.global
  concurrent.join(5)(Stream.constant(Stream.empty).covary[Task]).run.unsafeRun
}

object DanglingDequeueSanityTest extends App {
  import ExecutionContext.Implicits.global
  Stream.eval(async.unboundedQueue[Task,Int]).flatMap { q =>
    Stream.constant(1).flatMap { _ => Stream.empty mergeHaltBoth q.dequeue }
  }.run.unsafeRun
}

object AwakeEverySanityTest extends App {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global
  import TestUtil.scheduler
  time.awakeEvery[Task](1.millis).flatMap {
    _ => Stream.eval(Task(()))
  }.run.unsafeRun
}

object SignalDiscreteSanityTest extends App {
  import ExecutionContext.Implicits.global
  Stream.eval(async.signalOf[Task, Unit](())).flatMap { signal =>
    signal.discrete.evalMap(a => signal.set(a))
  }.run.unsafeRun
}

object SignalContinuousSanityTest extends App {
  import ExecutionContext.Implicits.global
  Stream.eval(async.signalOf[Task, Unit](())).flatMap { signal =>
    signal.continuous.evalMap(a => signal.set(a))
  }.run.unsafeRun
}

object ConstantEvalSanityTest extends App {
  import ExecutionContext.Implicits.global
  Stream.constant(()).flatMap { _ => Stream.eval(Task.delay(())) }.run.unsafeRun
}
