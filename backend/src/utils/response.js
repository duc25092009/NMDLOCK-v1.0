/**
 * NMDLock Standard API Response Helpers
 * Consistent JSON response format for all endpoints.
 */

function success(res, data = null, message = 'Success', statusCode = 200) {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
    timestamp: new Date().toISOString(),
  });
}

function created(res, data = null, message = 'Created successfully') {
  return success(res, data, message, 201);
}

function error(res, message = 'Internal server error', statusCode = 500, errors = null) {
  const response = {
    success: false,
    message,
    timestamp: new Date().toISOString(),
  };
  if (errors) response.errors = errors;
  return res.status(statusCode).json(response);
}

function badRequest(res, message = 'Bad request', errors = null) {
  return error(res, message, 400, errors);
}

function unauthorized(res, message = 'Unauthorized') {
  return error(res, message, 401);
}

function forbidden(res, message = 'Forbidden') {
  return error(res, message, 403);
}

function notFound(res, message = 'Resource not found') {
  return error(res, message, 404);
}

function rateLimited(res, message = 'Too many requests', retryAfter = 60) {
  res.set('Retry-After', String(retryAfter));
  return error(res, message, 429);
}

module.exports = {
  success,
  created,
  error,
  badRequest,
  unauthorized,
  forbidden,
  notFound,
  rateLimited,
};
