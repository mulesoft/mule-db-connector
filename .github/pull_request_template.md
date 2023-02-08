_Hi, thank you for your work. We understand that you want to merge your changes and move on from this issue, but we also want to maintain the safety, readability, and testability of our code. Because of this, we kindly ask that you confirm that you have fulfilled the prerequisites for each category of activity before merging your changes._

### If the Pull Request has a dependency update:

- [ ] I have read the Release Notes for the new version (and intermediate versions if the jump would include more than one). _Don't blindly trust that the dependency will honor SEMVER, always read carefully the release notes_
- [ ] Java 8+ support is maintained
- [ ] The new version has no vulnerabilities
- [ ] A team member reviewed the Pull Request.
- [ ] No backward compatibility is broken.
- [ ] If it is a "provided" dependency, it has been checked that if it is suggested, the version number is the same.

### For ALL the Releases:

- [ ] There is a task in GUS for this change.
- [ ] The corresponding Release Notes have been written and shared with the documentation team.
- [ ] The new code has tests and they have before and after methods to be sure that you are working with a clean environment and that you are leaving the environment clean after they finish.
- [ ] If you are using reflection, create a test to call the methods you are invoking, this way, if there is a change in the API, the test will be able to detect it.
- [ ] If the project has a parent pom and modules, the release pipeline only uploads the modules, so, please, make sure that the parent was deployed in the repositories before considering this release finished.
- [ ] Notify in the Slack Channel that the release is complete, so the Docs team can merge the Documentation and Release Notes PR. The docs team always checks in Anypoint Exchange that the connector is released before merging the release notes.
- [ ] Add the connector build to GUS, and assign that build to the GUS ticket.
- [ ] Create a change case request, based on the Change Case Management doc https://docs.google.com/document/d/1tMJlTTZLaXMLiYwxlMBFY1yJwBmejhc4-IP8HAXgDfY/edit#

**NOTE**: _(applies only for Core-Connectors connectors and modules) The release process ends when you merge the pull request that updates the support branch to the new snapshot, please don't forget to do this._

### Patch Version:

- [ ] The Pull Request has been peer-reviewed.
- [ ] No backward compatibility is broken.
- [ ] Documentation: (Required) Release Notes PR with compatibility table. Share it with the Docs team.

### Minor Version:

- [ ] Have a document with the specification of the release.
- [ ] Create a slack channel (eg: rl-conninfra-sftp-1.1.1-new-chiper) to coordinate the release of this version with the documentation team and the architects of our team (at least one week before the release). Share the spec doc with the docs team.
- [ ] The documentation fork has been done and has been updated with the changes related to the new functionalities.
- [ ] The pull request has been reviewed by a peer and the team leader.
- [ ] No backward compatibility is broken.
- [ ] Public Documentation: Minor releases nearly always require a revision of the documentation because the release usually includes new features as well as bug fixes. Even if there are no user-facing changes, documentation has to be versioned for every minor release.
  - [ ] (Required) Release Notes PR with compatibility table. Share it with the Docs team.
  - [ ] (Likely required) Reference guide. Run the script and share the file with the Docs team.
  - [ ] (Assess) Often a minor release impacts UI and examples, so the documentation (user guides, examples, troubleshooting guides) should be reviewed and updated accordingly. Notify the docs team if any updates are needed.

### Major Version:

- [ ] Have a document with the specification of the release.
- [ ] Create a slack channel (eg: rl-conninfra-sftp-2.0.0) to coordinate the release of this version with the documentation team and the architects of our team (at least one month before the release). Share the spec doc with the docs team.
- [ ] The documentation fork has been made and updated with the changes related to the new functionalities and possible compatibility problems with the previous version.
- [ ] The systems properties have been removed, detailing in the documentation the changes in the default behavior of the product.
- [ ] The TODO comments of the code have been reviewed.
- [ ] A colleague, the team leader, and the team architect have reviewed the Pull Request.
- [ ] Public Documentation: Major releases break backward compatibility and should never be released without documentation, as this breaks the user experience.
  - [ ] (Required) Upgrade and Migration Guide: Every major release must have a detailed upgrade guide (template) with information about what was changed and what steps the user has to take to ensure the connector works as expected. Share the details with the Docs team so they can update the guide.
  - [ ] (Required) Release Notes PR with compatibility table. Share it with the docs team.
  - [ ] (Required) Reference guide. Run the script and share the file with the Docs team.
  - [ ] User guide updates, new screenshots or examples (if applicable), share the updates with the docs team.
