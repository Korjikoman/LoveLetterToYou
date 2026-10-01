(function () {
    const safeMethods = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

    window.csrfFetch = function (input, init = {}) {
        const requestMethod = input instanceof Request ? input.method : "GET";
        const method = (init.method || requestMethod).toUpperCase();
        const requestUrl = input instanceof Request ? input.url : input;
        const targetUrl = new URL(requestUrl, window.location.href);

        if (safeMethods.has(method) || targetUrl.origin !== window.location.origin) {
            return fetch(input, init);
        }

        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.content;

        if (!token || !headerName) {
            return Promise.reject(new Error("CSRF token is not available on this page"));
        }
        const requestHeaders = input instanceof Request ? input.headers : undefined;
        const headers = new Headers(init.headers || requestHeaders);
        headers.set(headerName, token);

        return fetch(input, {...init, headers});
    };
})();
