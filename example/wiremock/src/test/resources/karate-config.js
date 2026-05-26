function fn() {
    var env = karate.env || 'local';
    var WireMockSupport = Java.type('example.wiremock.support.WireMockSupport');
    var dynamicBaseUrl = WireMockSupport.ensureStarted();
    var configuredBaseUrl = karate.properties['karate.config.baseUrl'];

    var config = {
        env: env,
        baseUrl: configuredBaseUrl && configuredBaseUrl !== 'http://localhost' ? configuredBaseUrl : dynamicBaseUrl,
        datasetPath: karate.properties['dataset.path'] || '',
        pollIntervalMillis: +(karate.properties['karate.config.pollIntervalMillis'] || 50),
        username: java.lang.System.getenv('API_USERNAME') || karate.properties['karate.user.username'] || 'demo-user',
        password: java.lang.System.getenv('API_PASSWORD') || karate.properties['karate.user.password'] || 'demo-password'
    };

    karate.callSingle('classpath:features/auth/bootstrap-login.feature', config);
    var login = karate.callSingle('classpath:features/auth/login.feature', config);
    config.authToken = login.token;

    karate.configure('headers', {
        Authorization: 'Bearer ' + config.authToken,
        Accept: 'application/json',
        'Content-Type': 'application/json',
        'X-Tenant': karate.properties['karate.config.tenant'] || 'local-sandbox'
    });
    karate.configure('afterFeature', function(){ WireMockSupport.stop(); });
    karate.configure('logPrettyRequest', true);
    karate.configure('logPrettyResponse', true);

    karate.log('env:', config.env, '| baseUrl:', config.baseUrl, '| datasetPath:', config.datasetPath);
    return config;
}
