# ChatGPT Code Review Gerrit Plugin

## Features

1. This plugin allows you to use ChatGPT for code review in Gerrit conveniently. After submitting a Patch Set, OpenAI
   will provide review feedback in the form of comments.
2. You can continue to ask ChatGPT by @{gerritUserName} or @{gerritEmailAddress} (provided that `gerritEmailAddress` is
   in the form "gerritUserName@<any_email_domain>") in the comments to further guide it in generating more targeted
   review comments.

## Getting Started

1. **Build:** Requires JDK 11 or higher, Maven 3.0 or higher.

   ```bash
   mvn -U clean package
    ```

   If the user needs to disable test just run
   ```bash
   mvn -U -DskipTests=true clean package
   ```

2. **Install:** Upload the compiled jar file to the `$gerrit_site/plugins` directory.

3. **Configure:** First, you need to create a ChatGPT user in Gerrit.
   Then, set up the basic parameters in your `$gerrit_site/etc/gerrit.config` file under the section

   `[plugin "chatgpt-code-review-gerrit-plugin"]`:

- `gptToken`: OpenAI GPT token.
- `gerritAuthBaseUrl`: The URL of your Gerrit instance, similar to `https://gerrit.local.team`.

  **NOTE**: Do not append "/a" authentication sub-path to the Gerrit URL.
- `gerritUserName`: Gerrit username of ChatGPT user.
- `gerritPassword`: Gerrit password of ChatGPT user.
- `globalEnable`: Default value is false. The plugin will only review specified repositories. If set to true, the plugin
   will by default review all pull requests.

   For enhanced security, consider storing sensitive information like gptToken and gerritPassword in a secure location
   or file. Detailed instructions on how to do this will be provided later in this document.

4. **Verify:** After restarting Gerrit, you can see the following information in Gerrit's logs:

   ```bash
   INFO com.google.gerrit.server.plugins.PluginLoader : Loaded plugin chatgpt-code-review-gerrit-plugin, version 1.0.0
   ```

   You can also check the status of the chatgpt-code-review-gerrit-plugin on Gerrit's plugin page as Enabled.

## Usage Examples

Examples of ChatGPT's code reviews and inline discussions are available at
https://wiki.amarulasolutions.com/opensource/chatgpt-gerrit.html

## Configuration Parameters

You have the option to establish global settings, or independently configure specific projects. If you choose
independent configuration, the corresponding project settings will override the global parameters.

### Global Configuration

To configure these parameters, you need to modify your Gerrit configuration file (`gerrit.config`). The file format is
as follows:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # Required parameters
    gptToken = {gptToken}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # Optional parameters
    gptModel = {gptModel}
    gptSystemPrompt = {gptSystemPrompt}
    ...
```

#### Secure Configuration

It is highly recommended to store sensitive information such as `gptToken` and `gerritPassword` in the `secure.config`
file. Please edit the file at $gerrit_site/etc/`secure.config` and include the following details:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    gptToken = {gptToken}
    gerritPassword = {gerritPassword}
```

If you wish to encrypt the information within the `secure.config` file, you can refer
to: https://gerrit.googlesource.com/plugins/secure-config

### Project Configuration

To add the following content, please edit the `project.config` file in `refs/meta/config`:

```
[plugin "chatgpt-code-review-gerrit-plugin"]
    # Required parameters
    gerritUserName = {gerritUserName}
    gerritAuthBaseUrl = {gerritAuthBaseUrl}
    ...

    # Optional parameters
    gptModel = {gptModel}
    gptSystemPrompt = {gptSystemPrompt}
    ...
```

#### Secure Configuration

Please ensure **strict control over the access permissions of `refs/meta/config`** since sensitive information such as
`gptToken` and `gerritPassword` is configured in the `project.config` file within `refs/meta/config`.

### Optional Parameters

- `gptModel`: The default model is gpt-3.5-turbo. You can also configure it to gpt-3.5-turbo-16k, gpt-4 or gpt-4-32k.
- `gptDomain`: The default ChatGPT domain is `https://api.openai.com`.
- `gptSystemPrompt`: You can modify the default system prompt ("Act as a PatchSet Reviewer") to your preferred prompt.
- `gptReviewTemperature`: Specifies the temperature setting for ChatGPT when reviewing a Patch Set, with a default
  setting of 0.2. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more
  focused and deterministic.
- `gptCommentTemperature`: Specifies the temperature setting for ChatGPT when replying to a comment, with a default
  setting of 1.0.
- `gptReviewPatchSet`: Set to true by default. When switched to false, it disables the automatic review of Patch Sets as
  they are created or updated.
- `gptReviewCommitMessages`: The default value is false. When enabled by setting to true, this option also verifies if
  the commit message matches with the content of the review.
- `gptFullFileReview`: Enabled by default. Activating this option sends both unchanged lines and changes to ChatGPT for
  review, offering additional context information. Deactivating it (set to false) results in only the changed lines
  being submitted for review.
- `gptStreamOutput`: The default value is false. Whether the response is expected in stream output mode or not.
- `maxReviewLines`: The default value is 1000. This sets a limit on the number of lines of code included in the review.
- `maxReviewFileSize`: Set with a default value of 10000, this parameter establishes a cap on the file size that can be
  included in reviews.
- `enabledUsers`: By default, every user is enabled to have their Patch Sets and comments reviewed. To limit review
  capabilities to specific users, list their usernames in this setting, separated by commas.
- `disabledUsers`: Functions oppositely to enabledUsers.
- `enabledGroups`: By default, all groups are permitted to have their Patch Sets and comments reviewed. To restrict
  review access to certain groups, specify their names in this setting, separating them with commas.
- `disabledGroups`: Operates in reverse to `enabledGroups`, excluding specified groups from reviews.
- `enabledTopicFilter`: Specifies a list of keywords that trigger ChatGPT reviews based on the topic of the Patch Set.
  When this setting is active, only Patch Sets and their associated comments containing at least one of these keywords
  in the topic are reviewed.
- `disabledTopicFilter`: Works in contrast to enabledTopicFilter, excluding Patch Sets and comments from review if their
  topics contain specified keywords.
- `enabledFileExtensions`: This limits the reviewed files to the given types. Default file extensions are ".py, .java,
  .js, .ts, .html, .css, .cs, .cpp, .c, .h, .php, .rb, .swift, .kt, .r, .jl, .go, .scala, .pl, .pm, .rs, .dart, .lua,
  .sh, .vb, .bat".
- `enabledVoting`: Initially disabled (false). If set to true, allows ChatGPT to cast a vote on each reviewed Patch Set
  by assigning a score.
- `patchSetCommentsAsResolved`: Initially set to false, this option leaves ChatGPT's Patch Set comments as unresolved,
  inviting further discussion. If activated, it marks ChatGPT's Patch Set comments as resolved.
- `inlineCommentsAsResolved`: Initially set to false, this option leaves ChatGPT's inline comments as unresolved,
  inviting further discussion. If activated, it marks ChatGPT's inline comments as resolved.
- `votingMinScore`: The lowest possible score that can be given to a Patch Set (Default value: -1).
- `votingMaxScore`: The highest possible score that can be given to a Patch Set (Default value: +1).

#### Optional Parameters for Global Configuration only

- `globalEnable`: Set to false by default, meaning the plugin will review only designated repositories. If enabled, the
  plugin will automatically review all pull requests by default (not recommended in production environments).
- `enabledProjects`: The default value is an empty string. If globalEnable is set to false, the plugin will only run in
  the repositories specified here. The value should be a comma-separated list of repository names, for example:
  "project1,project2,project3".

#### Optional Parameters for Project Configuration only

- `isEnabled`: The default is false. If set to true, the plugin will review the Patch Set of this project.

## Commands

- `/review`: when used in a comment directed at ChatGPT on any Change Set, triggers a review of the full Change Set. A
  vote is cast on the Change Set if the voting feature is enabled and the ChatGPT Gerrit user is authorized to vote on
  it.
- `/review_last`: when used in a comment directed at ChatGPT on any Change Set, triggers a review of the last Patch Set
  of the Change Set. Unlike `/review`, this command does not result in casting or updating votes.

### Command Options

- `--filter=[true/false]`: Controls the filtering of duplicate and conflicting comments, defaulting to "true" to apply
  filters.

## Testing

- You can run the unit tests in the project to familiarize yourself with the plugin's source code.
- If you want to individually test the Gerrit API or the ChatGPT API, you can refer to the test cases in
  CodeReviewPluginIT.

## License

Apache License 2.0
