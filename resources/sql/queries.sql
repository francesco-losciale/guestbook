-- START:posts
-- START:save-message!
-- :name save-message! :<! :1
-- :doc creates a new message using the name and message keys
INSERT INTO posts
(author, name, message)
VALUES (:author, :name, :message)
RETURNING *;
-- END:save-message!

-- :name get-messages :? :*
-- :doc selects all available messages
SELECT * from posts
-- END:posts

-- START:users
-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
INSERT INTO users
(login, password)
VALUES (:login, :password)

-- :name get-user-for-auth* :? :1
-- :doc selects a user for authentication
SELECT * FROM users
WHERE login = :login
-- END:users
