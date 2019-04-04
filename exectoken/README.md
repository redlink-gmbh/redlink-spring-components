# Execution Token

Library that allows to synchronize executions by using tokens managed in MongoDB.

So when you are operating in a cloud setting with multiple workers and you want to ensure that a specific operation is only executed once at a time you can simple request an execution token with a given name.

    try (ExecutionToken token = etService.obtain(MyTask.class.gerName())){
        //do all the work required for my-task
    }
    
By default the Token will be kept for 5 minutes. So if your task will run longer you will need to parse the Duration as
second parameter of the `obtain` method.

__NOTE:__ releasing Tokens after the lockout period uses the Mongo TTL feature. As stated by the [documentation](https://docs.mongodb.com/manual/core/index-ttl/#timing-of-the-delete-operation) the background task runs once every 60 seconds. So the actual lockout period of a Token might be up to 60 seconds longer as the stated period!


If your task performs multiple tasks you can also keep using a short lockout period but instead renewing the token by
calling `token.renew()`.

In any case as soon as the execution completes (hence the `try` block is left) the token will be released as the Token
implements `AutoCloseable`.

However in some use-cases this might not be desirable. So it is possible to disable releasing the lock on close by
calling `token.setReleaseOnClose(false)`.

    try (ExecutionToken token = etService.obtain(MyTask.class.gerName(),
            Duration.ofDays(6).plus(Duration.ofHours(12))){
        //keep the lockout around after execution of the task completed
        token.setReleaseOnClose(false);
        
        //do all the work required for my-task
        
    } catch(MyException e){
        //maybe release the token if the execution fails
        token.setReleaseOnClose(true);
    }

The above example allows to lockout (re-)sending of mails for 6 days and 12 hours. So given task will be prevented from
execution for about a week after is succeeded the last time
    