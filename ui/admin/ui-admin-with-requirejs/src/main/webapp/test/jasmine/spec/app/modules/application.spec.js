/*global define, describe, beforeEach, afterEach, it, xit, expect, spyOn, setFixtures, sandbox */
/*jslint nomen:false */

describe('Application', function () {

    beforeEach(function () {

    });

    afterEach(function () {
        // make sure you clean up any test doubles
    });

    it("Should be a passing spec.", function () {
        expect(true).toBeTruthy();
    });

    describe('Model', function () {

        beforeEach(function () {

        });

        afterEach(function () {
            // make sure you clean up any test doubles
        });

        it("Should create a new User model.", function() {
            var User = require('modules/security/user');
            var blah = new User.Model();
            expect(blah).toBeDefined();
        });

    });

    describe('Collection', function () {

        beforeEach(function () {

        });

        afterEach(function () {
            // make sure you clean up any test doubles
        });

    });

    describe('Views', function () {

        beforeEach(function () {

        });

        afterEach(function () {
            // make sure you clean up any test doubles
        });

    });

    describe('Router', function () {

        beforeEach(function () {

        });

        afterEach(function () {
            // make sure you clean up any test doubles
        });

    });
});
