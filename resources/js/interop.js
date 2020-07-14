/**
 * HTTP Response data object, contains information about response content, headers, status, etc.
 */
function HttpResponse(response) {
    // noinspection EqualityComparisonWithCoercionJS

    /**
     * Response content, it is a string or JSON object if response content-type is json.
     * @type {string|object}
     */
    this.body = response.contentType.mimeType == 'application/json' ?
        JSON.parse(response.body) : response.body;

    /**
     * Response status, e.g. 200, 404, etc.
     * @type {int}
     */
    this.status = Number(response.status);

    /**
     * Response headers storage.
     */
    this.headers = response.headers;

    /**
     * Value of 'Content-Type' response header.
     */
    this.contentType = response.contentType;
}

function Variables() {
    this.__state = {}

    this.set = function (name, value) {
        this.__state[name] = value;
    }
    this.get = function (name) {
        return this.__state[name];
    }
    this.isEmpty = function (name) {
        return this.__state[name] !== undefined;
    }
    this.clear = function (name) {
        delete this.__state[name];
    }
    this.clearAll = function () {
        this.__state = {}
    }
}

function HttpClient() {
    this.global = new Variables();

    this.test = function (name, fn) {
        try {
            fn();
        } catch (e) {
            throw ("In test `" + name + "` - " + e)
        }
    }

    this.assert = function (assertion, msg) {
        if (!assertion) {
            throw msg;
        }
    }
}

var client = new HttpClient();