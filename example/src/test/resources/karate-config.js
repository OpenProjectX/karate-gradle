function fn() {
    var env = karate.env || 'staging';

    // Base config — can be overridden by environment YAML via karate.config.* system properties
    var config = {
        env:         env,
        baseUrl:     'https://jsonplaceholder.typicode.com',
        datasetPath: karate.properties['dataset.path'] || ''
    };

    // Apply env-specific overrides forwarded by the plugin as karate.config.<key>=<value>
    var baseUrlOverride = karate.properties['karate.config.baseUrl'];
    if (baseUrlOverride) {
        config.baseUrl = baseUrlOverride;
    }

    karate.log('env:', config.env, '| baseUrl:', config.baseUrl, '| datasetPath:', config.datasetPath);

    return config;
}
