# Deploying

## Snapshots

```ruby
mvn clean deploy
```

## Staging

```ruby
mvn release:clean
mvn release:prepare
mvn release:perform
```

## Releasing

Open a browser and go to Nexus UI with address: [https://oss.sonatype.org/](https://oss.sonatype.org/).

To close a staging repository:

1. Login to the Nexus UI.
1. Go to Staging Repositories page.
1. Select a staging repository.
1. Click the Close button.

You will be asked to provide a description for this staging close operation. If the staging repository is closed successfully, you will get a notification email and confirmation via the ui.

To make sure the deployed artifacts meet the [Central Sync Requirements](https://docs.sonatype.org/display/Repository/Central+Sync+Requirements), Nexus has a few staging rules running on Staging Close and Release. These rules will validate your deployment has correct POM, javadoc, source, pgp signatures, and checksums etc. If your deployment has any problem, you will get a report

When you are sure the closed staging repository has no problem, click the Release button. You will be asked to input a description for this release. Staging rules will run again. If the closed staging repository is released successfully, you will get a notification email. Now your artifacts are in the **Releases** repository, which is synced to The Central Repository roughly every 2 hours. Now you can browse or search your artifacts from Nexus 

# Resources

[https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide](https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide)

