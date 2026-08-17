(function () {
  var endpoint =
    "https://7m2j372rk8.execute-api.us-east-1.amazonaws.com/prod/visit";
  if (typeof navigator.sendBeacon !== "function") {
    return;
  }
  navigator.sendBeacon(
    endpoint,
    JSON.stringify({
      path: location.pathname,
      referer: document.referrer || null,
      userAgent: navigator.userAgent
    })
  );
})();
