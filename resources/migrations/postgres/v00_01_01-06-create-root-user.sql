/* Copyright (C) 2019  Christopher R. Reyes
 *
 * This file is part of Task Tracker.
 *
 * Task Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Task Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Task Tracker.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Creates a user and database named "root" for the purposes of the health check
 */

create user root;
create database root with owner root;
