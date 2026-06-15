package com.srivatchan.apiexplorer.model;

public enum BitbucketResource {

    // Workspace & user (no repo needed)
    CURRENT_USER(
            "Current User",
            "https://api.bitbucket.org/2.0/user",
            false, false),

    WORKSPACE_INFO(
            "Workspace Info",
            "https://api.bitbucket.org/2.0/workspaces/{workspace}",
            true, false),

    LIST_REPOS(
            "List Repositories",
            "https://api.bitbucket.org/2.0/repositories/{workspace}",
            true, false),

    // Repo-level (workspace + repoSlug)
    GET_REPO(
            "Get Repository",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}",
            true, true),

    LIST_FORKS(
            "List Repo Forks",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/forks",
            true, true),

    BROWSE_ROOT(
            "Browse Root Directory",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/src",
            true, true),

    LIST_BRANCHES(
            "List Branches",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/refs/branches",
            true, true),

    LIST_COMMITS(
            "List All Commits",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/commits",
            true, true),

    LIST_DEPLOYMENTS(
            "List Deployments",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/deployments",
            true, true),

    LIST_ENVIRONMENTS(
            "List Environments",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/environments",
            true, true),

    LIST_WEBHOOKS(
            "List Repo Webhooks",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/hooks",
            true, true),

    // File browser - dynamic path entered by user at call time
    BROWSE_FILE(
            "Browse File / Directory",
            "https://api.bitbucket.org/2.0/repositories/{workspace}/{repoSlug}/src/{filePath}",
            true, true);

    private final String label;
    private final String urlTemplate;
    private final boolean needsWorkspace;
    private final boolean needsRepoSlug;

    BitbucketResource(String label, String urlTemplate,
                      boolean needsWorkspace, boolean needsRepoSlug) {
        this.label       = label;
        this.urlTemplate = urlTemplate;
        this.needsWorkspace  = needsWorkspace;
        this.needsRepoSlug   = needsRepoSlug;
    }

    public String getLabel() { return label; }
    public String getUrlTemplate() { return urlTemplate; }
    public boolean isNeedsWorkspace() { return needsWorkspace; }
    public boolean isNeedsRepoSlug() { return needsRepoSlug; }

    /** Returns true only for BROWSE_FILE — the caller must supply a filePath. */
    public boolean requiresFilePath() {
        return this == BROWSE_FILE;
    }

    public String buildUrl(String workspace, String repoSlug, String filePath) {
        String url = urlTemplate;
        if (needsWorkspace)  url = url.replace("{workspace}", workspace);
        if (needsRepoSlug)   url = url.replace("{repoSlug}",  repoSlug);
        if (this == BROWSE_FILE && filePath != null && !filePath.isBlank()) {
            url = url.replace("{filePath}", filePath.replaceAll("^/+", ""));
        }
        return url;
    }
}
