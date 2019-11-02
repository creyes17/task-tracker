# task-tracker

Keeps track of task estimates and subtasks.

Implements https://arxiv.org/pdf/0806.3115.pdf

## Dependencies

You'll need to install [Docker](https://www.docker.com/get-started) (along with relevant CLIs)

You'll also need to get setup with AWS and store your database configuration in SecretsManager

- TODO: Figure out how to get this spun up with docker-compose locally without AWS

## Installation

Clone from http://www.github.com/creyes17/task-tracker

## Usage

TODO: Implement and flesh this out.

First make sure you've created the compiled uberjar with `lein uberjar`.

Then start the default postgres service with `docker-compose up`. You can connect to the running postgres instance with `docker-compose exec postgres psql`.

- Note: make sure to stop any running postgres services via brew with `brew services stop postgres`. Otherwise you won't be able to connect to postgres on the docker image.

This also starts the backend web service running on port equal to your `C17_TASK_TRACKER_BACKEND_PORT` environment variable (default 5000). You can see that the service is healthy with `curl http://localhost:${C17_TASKTRACKER_BACKEND_PORT:-5000}/.internal/is_healthy`.

- Right now, this is the only endpoint specified. More to come before version v1.0.0.

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

## License

Copyright &copy; 2019 Christopher R Reyes

This project is licensed under the GNU General Public License v3.0.
