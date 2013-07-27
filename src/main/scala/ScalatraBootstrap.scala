import com.github.yzgw.app._
import org.scalatra._
import javax.servlet.ServletContext
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new Journey(), "/*")
    context.initParameters("org.scalatra.environment") = "production"
    
  }
}
