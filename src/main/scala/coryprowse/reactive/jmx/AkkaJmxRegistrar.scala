package coryprowse.reactive.jmx

import java.lang.management.ManagementFactory
import javax.management._

import akka.actor.Actor

object AkkaJmxRegistrar {
  private lazy val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer

  @throws[InstanceAlreadyExistsException]
  @throws[MBeanRegistrationException]
  @throws[RuntimeMBeanException]
  @throws[RuntimeErrorException]
  @throws[NotCompliantMBeanException]
  @throws[RuntimeOperationsException]
  def registerToMBeanServer(actor: Actor, objName: ObjectName): ObjectInstance = mbs.registerMBean(actor, objName)

  @throws[RuntimeOperationsException]
  @throws[RuntimeMBeanException]
  @throws[RuntimeErrorException]
  @throws[InstanceNotFoundException]
  @throws[MBeanRegistrationException]
  def unregisterFromMBeanServer(objName: ObjectName): Unit = mbs.unregisterMBean(objName)
}