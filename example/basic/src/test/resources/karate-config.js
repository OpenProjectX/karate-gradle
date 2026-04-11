function fn() {
    var env = karate.env || 'staging';
    var commit = karate.properties['karate.commit'] || 'local';

    // Base config — can be overridden by environment YAML via karate.config.* system properties
    var config = {
        env:         env,
        baseUrl:     'https://jsonplaceholder.typicode.com',
        datasetPath: karate.properties['dataset.path'] || '',
        commit:      commit,
        workflow:    karate.properties['karate.workflow'] || 'regression'
    };

    // Apply env-specific overrides forwarded by the plugin as karate.config.<key>=<value>
    var baseUrlOverride = karate.properties['karate.config.baseUrl'];
    if (baseUrlOverride) {
        config.baseUrl = baseUrlOverride;
    }

    config.readTimeout = karate.properties['karate.config.readTimeout'] || 5000;
    config.connectTimeout = karate.properties['karate.config.connectTimeout'] || 3000;
    config.tenant = karate.properties['karate.config.tenant'] || 'public';

    karate.configure('headers', {
        Accept: 'application/json',
        'X-Test-Commit': config.commit,
        'X-Test-Workflow': config.workflow
    });

    karate.log('env:', config.env, '| baseUrl:', config.baseUrl, '| datasetPath:', config.datasetPath, '| commit:', config.commit);

    return config;
}
