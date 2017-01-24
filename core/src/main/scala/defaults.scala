package dispatch

import io.netty.util.{Timer, HashedWheelTimer}
import org.asynchttpclient._

object Defaults {
  implicit def executor = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val timer: Timer = InternalDefaults.timer
}

private [dispatch] object InternalDefaults {
  private lazy val underlying = BasicDefaults

  lazy val config = underlying.builder.build()
  def client = new DefaultAsyncHttpClient(config)
  lazy val timer = underlying.timer

  private trait Defaults {
    def builder: DefaultAsyncHttpClientConfig.Builder
    def timer: Timer
  }

  /** Sets a user agent, no timeout for requests  */
  private object BasicDefaults extends Defaults {
    lazy val timer = new HashedWheelTimer()
    def builder = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent("Dispatch/%s" format BuildInfo.version)
      .setRequestTimeout(-1) // don't timeout streaming connections
  }

  /** Uses daemon threads and tries to exit cleanly when running in sbt  */
  private object SbtProcessDefaults extends Defaults {
    def builder = {
      val shuttingDown = new juc.atomic.AtomicBoolean(false)

      def shutdown(): Unit = {
        if (shuttingDown.compareAndSet(false, true)) {
          nioClientSocketChannelFactory.releaseExternalResources()
          timer.stop()
        }
        ()
      }
      /** daemon threads that also shut down everything when interrupted! */
      lazy val interruptThreadFactory = new juc.ThreadFactory {
        def newThread(runnable: Runnable) = {
          new Thread(runnable) {
            setDaemon(true)
            /** only reliably called on any thread if all spawned threads are daemon */
            override def interrupt() = {
              shutdown()
              super.interrupt()
            }
          }
        }
      }
      lazy val nioClientSocketChannelFactory = {
        val workerCount = 2 * Runtime.getRuntime().availableProcessors()
        new NioClientSocketChannelFactory(
          juc.Executors.newCachedThreadPool(interruptThreadFactory),
          1,
          new NioWorkerPool(
            juc.Executors.newCachedThreadPool(interruptThreadFactory),
            workerCount
          ),
          timer
        )
      }

      val config = new NettyAsyncHttpProviderConfig().addProperty(
        "socketChannelFactory",
        nioClientSocketChannelFactory
      )
      config.setNettyTimer(timer)
      BasicDefaults.builder.setAsyncHttpClientProviderConfig(config)
    }
    lazy val timer = new HashedWheelTimer(DaemonThreads.factory)
  }
}

object DaemonThreads {
  /** produces daemon threads that won't block JVM shutdown */
  val factory = new juc.ThreadFactory {
    def newThread(runnable: Runnable): Thread ={
      val thread = new Thread(runnable)
      thread.setDaemon(true)
      thread
    }
  }
  def apply(threadPoolSize: Int) =
    juc.Executors.newFixedThreadPool(threadPoolSize, factory)
}
