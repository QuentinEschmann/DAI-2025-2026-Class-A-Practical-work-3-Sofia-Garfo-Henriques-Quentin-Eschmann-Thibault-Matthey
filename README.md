# DAI-2025-2026-Class-A-Practical-work-3-Sofia-Garfo-Henriques-Quentin-Eschmann-Thibault-Matthey
DAI-2025-2026-Class-A-Practical-work-3


## Cache implementation

The cache is currently implemented for the inventory and the user management system. When you list an item (or all items) or a user (or all users), you will get en Etag in your response. This Etag can be used as header in your next request. If the requested ressourcehas not been modified since your last call, the server will send a 304 response wich means that you can reuse the data queried earlier. This system reduces server response time and ressources usage on the server. 