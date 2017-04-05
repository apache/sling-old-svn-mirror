# Sling Event (Jobs) bundle.

For user documentation see https://sling.apache.org/documentation/bundles/apache-sling-eventing-and-job-handling.html. 
This README contains information on the bundle, APIs and implementation details.

# Bundle

Sling Event contains support for Jobs. It provides an Api for Job, JobManager and Queue, as well as consumer Apis for a 
JobConsumer. There are ancillary APIs to support the work of these core interfaces. The core APIs are exported from 
org.apache.sling.event.jobs with the consumers exported from org.apache.sling.event.jobs.consumer.


# Design and implementation


## Processing model

Jobs are created using the JobManager API. When a Job is created the JobManager writes an entry into the resource tree
(usually backed up by JCR) via the ResourceResolver. 

For notification of interested parties - not for processing the jobs(!) - adding a job emits an OSGi 
Event on org/apache/sling/api/resource/Resource/ADDED topic, which is picked up by the 
[NewJobSender](src/main/java/org/apache/sling/event/impl/jobs/notifications/NewJobSender.java) which emits a new OSGi
Event on the org/apache/sling/event/notification/job/ADDED topic.

Similar other notification events are sent out if the state of a job changes. However these events are just FYI events.
The events used are contained in [NotificationConstants.java](src/main/java/org/apache/sling/event/jobs/NotificationConstants.java).

The QueueManager which identifies the queue from s job is periodically scanning the resource tree for new jobs. In
addition it listens to the org/apache/sling/event/notification/job/ADDED topic for optimization and picking up new
jobs faster (than by scanning). Once a new job is found, the manager triggers the JobQueueImpl to start processing. 
Various other operations ensure that the JobQueueImpl runs jobs according to its configuration. These are either 
periodic maintenance classes or triggered by calls to the QueueManager or triggered by Jobs on the queue changing state.

The queue is maintained by the JobManagerImpl, but each Queue is managed by a JobQueueImpl that receives calls from the
QueueManager to process jobs. Any thread may update the persisted job state, by resolving the Job name and performing the operation.

## Storage

The JobManager uses the resource tree and therefore by default the JCR repository provided by Oak for persisting Jobs. The 
content tree structure was developed in conjunction with advice from the Oak team to avoid write concurrency issues and the 
need for maintaining in Oak repository locks. To avoid OakMergeConflicts on write, each job gets a UUID. If a new job is
created on a Sling instance, this instance decides - based on configuration - which Sling instance will process the job
and writes the new job to an area dedicated to that instance (/var/eventing/jobs/assigned/<SlingID>). Each Sling instance
only reads and modifies its own subtree under /var/eventing/jobs/assigned/<SlingID>. This prevents more than one instance from
attempting to process a Job at the same time. This means that when a Job is started, the path of the job is formed from
the target SlingID and the queue name. The target SlingID is formed from the queue configuration informed
by properties contained within Topology. Every Sling instance advertises is capabilities for processing jobs via topology,
hence the JobManager pre-allocates Jobs to instance when the job is created.

The JobQueueImpl then receives the job. The JobQueueImpl only considers jobs allocated to the local instance, and runs
those jobs. 

Since the SlingID is part of the JobID, there is no risk of 2 instances writing to the same job at the same time when the 
job is allocated or re-allocated to an instance. In the case of first allocation, the creating sling instance will perform 
the write operation and the target sling instance wont know about the job until after Oak commits. In the case of re-allocation
the target sling instance is dead, the cluster leader performs the write and the new target sling instnance wont see the job 
until after Oak commits.

## Topology changes

When a Sling instance in a cluster is shutdown, it will stop processing all the jobs allocated to it. When it shuts down
a Topology change event propagates and the cluster leader scans all instances under /var/eventing/jobs/assigned/ to see
if there are any instances that don't exist any more. If there are, the topology leader moves those jobs to a different
node by deleting the Oak node and writing a new node into the new targetId assigned location. Any jobs that cant be re-assigned
are written to the unassigned location.

## Known issues with current implementation and design

These issues may have been addressed since this document was written, if they have please remove the known issues.

1. Pre-allocation of jobs to queues bound to instances will not ensure load is distributed amongst available instances
especially when the queues are large, as jobs complexity and resource requirements vary wildly.
2. When the topology changes, with many jobs the cost of reallocating jobs may be prohibitive.


# Scheduled Jobs.

In addition to one off jobs the bundle has support for scheduled jobs. The schedule is stored in /var/eventing/scheduled-jobs, 
and the cluster leader uses the Sling commons Scheduler service to run a schedules which add jobs to the appropriate queues
using the job manager. For info see org.apache.sling.event.impl.jobs.scheduling.JobSchedulerImpl.execute which is called by the 
Sling commons Scheduler service.


 
