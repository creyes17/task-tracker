# task-tracker

Keeps track of task estimates and subtasks.

Implements https://arxiv.org/pdf/0806.3115.pdf

## Dependencies

You'll need to install [Docker](https://www.docker.com/get-started) (along with relevant CLIs)

You'll also need to get setup with AWS and store your database configuration in SecretsManager

- TODO: Figure out how to get this spun up with docker-compose locally without AWS

## Installation

Download from http://www.github.com/creyes17/task-tracker

## Usage

TODO: Implement and flesh this out.

To start the default postgres service, run `docker-compose up`. You can then connect to the running postgres instance with `docker-compose exec postgres psql`.

To create a new task (after starting the postgres service with `docker-compose up`), do `lein run`.

- Note: make sure to stop any running postgres services via brew with `brew services stop postgres`. Otherwise you won't be able to connect to postgres on the docker image.

  \$ java -jar task-tracker-0.1.0-standalone.jar

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

## License

Copyright &copy; 2019 Christopher R Reyes

This project is licensed under the GNU General Public License v3.0.
