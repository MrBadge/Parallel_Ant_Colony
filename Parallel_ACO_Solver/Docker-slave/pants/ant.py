import bisect
import functools
import itertools
import math
import random

from .world import World


@functools.total_ordering
class Ant:
    """
    A single independent finder of solutions to a :class:`World`.

    Each :class:`Ant` finds a solution to a world one move at a time.  They
    also represent the solution they find, and are capable of reporting which
    nodes and edges they visited, in what order they were visited, and the
    total length of the solution.

    Two properties govern the decisions each :class:`Ant` makes while finding
    a solution: *alpha* and *beta*. *alpha* controls the importance placed on
    pheromone while *beta* controls the importance placed on distance. In 
    general, *beta* should be greater than *alpha* for best results.
    :class:`Ant`\s also have a *uid* property that can be used to identify a
    particular instance.

    Using the :func:`initialize` method, each :class:`Ant` *must be 
    initialized* to a particular :class:`World`, and optionally may be given an
    initial node from which to start finding a solution. If a starting node is
    not given, one is chosen at random. Thus a few examples of instantiation
    and initialization might look like:

    .. code-block:: python

        ant = Ant()
        ant.initialize(world)
        
    .. code-block:: python

        ant = Ant().initialize(world)

    .. code-block:: python

        ant = Ant(alpha=0.5, beta=2.25)
        ant.initialize(world, start=world.nodes[0])

    .. note::

        The examples above assume the world has already been created!

    Once an :class:`Ant` has found a solution (or at any time), the solution
    may be obtained and inspected by accessing its ``tour`` property, which
    returns the nodes visited in order, or its ``path`` property, which 
    returns the edges visited in order. Also, the total distance of the 
    solution can be accessed through its ``distance`` property. :class:`Ant`\s
    are even sortable by their distance:
    
    .. code-block:: python
    
        ants = [Ant() for ...]
        # ... have each ant in the list solve a world
        ants = sorted(ants)
        for i in range(1, len(ants)):
            assert ants[i - 1].distance < ants[i].distance
            
    :class:`Ant`\s may be cloned, which will return a shallow copy while not 
    preserving the *uid* property. If this behavior is not desired, simply use
    the :func:`copy.copy` or :func:`copy.deepcopy` methods as necessary.
    
    The remaining methods mainly govern the mechanics of making each move.
    :func:`can_move` determines whether all possible moves have been made, 
    :func:`remaining_moves` returns the moves not yet made, :func:`choose_move`
    returns a single move from a list of moves, :func:`make_move` actually
    performs the move, and :func:`weigh` returns the weight of a given move.
    The :func:`move` method governs the move-making process by gathering the
    remaining moves, choosing one of them, making the chosen move, and 
    returning the move that was made.
    """
    uid = 0

    def __init__(self, alpha=1, beta=3, capacity=100):
        """Create a new Ant for the given world.

        :param float alpha: the relative importance of pheromone (default=1)
        :param float beta: the relative importance of distance (default=3)
        """
        self.uid = self.__class__.uid
        self.__class__.uid += 1
        self.world = None
        self.alpha = alpha
        self.beta = beta
        self.start = None
        self.distance = 0
        self.visited = []
        self.unvisited = []
        self.traveled = []
        self.capacity = 0
        self.max_capacity = capacity
        self.tis, self.tie = 0, 0
        self._timeline = []
        self.heuristicF, self.heuristicW = [], []
        self._remaining_moves = []

    @property
    def current_time(self):
        if not self._timeline:
            return 0
        else:
            return self._timeline[-1]

    def initialize(self, world, start=None):
        """Reset everything so that a new solution can be found.

        :param World world: the world to solve
        :param Node start: the starting node (default is chosen randomly)
        :return: `self`
        :rtype: :class:`Ant`
        """
        self.world = world
        if start is None:
            self.start = random.randrange(len(self.world.nodes))
        else:
            self.start = start
        self.distance = 0
        self.capacity = 0
        self.tis = self.world.real_nodes()[0].tis
        self._timeline = [self.tis]
        self.tie = float('inf')
        self.visited = [self.start]
        self.unvisited = [n for n in self.world.nodes if n != self.start]
        self.traveled = []
        self.heuristicF, self.heuristicW = [], []
        self._remaining_moves = []
        return self

    def clone(self):
        """Return a shallow copy with a new UID.
        
        If an exact copy (including the uid) is desired, use the 
        :func:`copy.copy` method.

        :return: a clone
        :rtype: :class:`Ant`
        """
        ant = Ant(alpha=self.alpha, beta=self.beta, capacity=self.capacity)
        ant.world = self.world
        ant.start = self.start
        ant.visited = self.visited[:]
        ant.unvisited = self.unvisited[:]
        ant.traveled = self.traveled[:]
        ant.distance = self.distance
        ant._timeline = self._timeline
        ant.tis = self.tis
        ant.tie = self.tie
        return ant

    @property
    def node(self):
        """Most recently visited node."""
        try:
            return self.visited[-1]
        except IndexError:
            return None

    @property
    def tour(self):
        """Nodes visited by the :class:`Ant` in order."""
        return [self.world.data(i) for i in self.visited]

    @property
    def path(self):
        """Edges traveled by the :class:`Ant` in order."""
        return [edge for edge in self.traveled]

    def get_node(self, index):
        return self.world.real_nodes()[index]

    def __eq__(self, other):
        """Return ``True`` if the distance is equal to the other distance.
        
        :param Ant other: right-hand argument
        :rtype: bool
        """
        return self.distance == other.distance

    def __lt__(self, other):
        """Return ``True`` if the distance is less than the other distance.
        
        :param Ant other: right-hand argument
        :rtype: bool
        """
        return self.distance < other.distance

    def can_move(self):
        """Return ``True`` if there are moves that have not yet been made.
    
        :rtype: bool
        """
        # This is only true after we have made the move back to the starting
        # node or running out of capacity or time.
        return len(self.traveled) != len(self.visited) and \
               self.capacity < self.max_capacity and \
               any([i != self.node and self.current_time + self.world.data(self.node, i).time <=
                    self.world.real_nodes()[i].tie for i, n in
                    enumerate(self.world.real_nodes())])  # self.world.real_nodes()[i].tis <=

    def move(self):
        """Choose, make, and return a move from the remaining moves.
        
        :return: the :class:`Edge` taken to make the move chosen
        :rtype: :class:`Edge`
        """
        remaining = self.remaining_moves()
        if not remaining:
            return None
        self.update_heuristics()
        choice = self.choose_move(remaining)
        if not self.enough_weight_and_time(choice):
            return self.make_move(self.start)
        return self.make_move(choice)

    def update_heuristics(self):
        def heuristicF():
            F = [self.get_node(i).tie - max(self.get_node(self.node).tis, self.current_time) - self.get_node(
                self.node).st - self.world.data(self.node, i).time for i in rm]
            mu = sum(F) / len(F)
            return [1 / (1 + math.exp(0.0001 * (f - mu))) for f in F]

        def heuristicW():
            W = [abs(self.get_node(i).tis - self.current_time - self.get_node(self.node).st - self.world.data(self.node,
                                                                                                              i).time)
                 for i in rm]
            mu = sum(W) / len(W)
            return [1 / (1 + math.exp(0.01 * (w - mu))) for w in W]

        rm = self.remaining_moves()
        if not rm:
            return []
        F = [self.get_node(i).tie - max(self.get_node(self.node).tis, self.current_time) - self.get_node(
            self.node).st - self.world.data(self.node, i).time for i in rm]
        mu_f = sum(F) / len(F)
        W = [abs(self.get_node(i).tis - self.current_time - self.get_node(self.node).st - self.world.data(self.node,
                                                                                                          i).time)
             for i in rm]
        mu_w = sum(W) / len(W)
        h_f, h_w = [], []
        for i, f in enumerate(F):
            h_f.append(1 / (1 + math.exp(0.0001 * (f - mu_f))))
            h_w.append(1 / (1 + math.exp(0.01 * (W[i] - mu_w))))
        self.heuristicF = h_f
        self.heuristicW = h_w

    def enough_weight_and_time(self, dest):
        rn = self.world.real_nodes()
        return self.capacity + rn[dest].demand <= self.max_capacity and self.current_time + self.world.data(
            self.node, dest).time <= rn[dest].tie  # rn[dest].tis <=

    def remaining_moves(self):
        """Return the moves that remain to be made.
        
        :rtype: list
        """

        # return self.unvisited
        if not self._remaining_moves:
            self._remaining_moves = list(filter(self.enough_weight_and_time, self.unvisited))
        return self._remaining_moves

    def choose_move(self, choices):
        """Choose a move from all possible moves.
        
        :param list choices: a list of all possible moves
        :return: the chosen element from *choices*
        :rtype: node
        """
        if len(choices) == 0:
            return None
        if len(choices) == 1:
            return choices[0]

        # Find the weight of the edges that take us to each of the choices.
        weights = []
        for move in choices:
            edge = self.world.edges[self.node, move]
            weights.append(self.weight(edge))

        # Choose one of them using a weighted probability.
        total = sum(weights)
        cumdist = list(itertools.accumulate(weights)) + [total]
        return choices[bisect.bisect(cumdist, random.random() * total)]

    def make_move(self, dest):
        """Move to the *dest* node and return the edge traveled.
        
        When *dest* is ``None``, an attempt to take the final move back to the
        starting node is made. If that is not possible (because it has 
        previously been done), then ``None`` is returned.
        
        :param node dest: the destination node for the move
        :return: the edge taken to get to *dest*
        :rtype: :class:`Edge`
        """
        # Since self.node simply refers to self.visited[-1], which will be
        # changed before we return to calling code, store a reference now.
        ori = self.node

        # When dest is None, all nodes have been visited but we may not
        # have returned to the node on which we started. If we have, then
        # just do nothing and return None. Otherwise, set the dest to the
        # node on which we started and don't try to move it from unvisited
        # to visited because it was the first one to be moved.
        if dest is None or ori == dest:
            if not self.can_move() or self.start == self.node:
                return None
            dest = self.start  # last move is back to the start
        elif dest != self.start:
            self.visited.append(dest)
            self.unvisited.remove(dest)

        edge = self.world.edges[ori, dest]
        self.traveled.append(edge)
        self.distance += edge.length
        self.capacity += int(edge.end.demand)
        self._timeline.append(max(edge.end.tis, self.current_time + edge.time) + edge.end.st)
        self._remaining_moves = []
        return edge

    def weight(self, edge):
        """Calculate the weight of the given *edge*.
        
        The weight of an edge is simply a representation of its perceived value
        in finding a shorter solution. Larger weights increase the odds of the
        edge being taken, whereas smaller weights decrease those odds.
        
        :param Edge edge: the edge to weigh
        :return: the weight of *edge*
        :rtype: float
        """

        pre = 1 / (edge.length or 1)
        post = edge.pheromone
        start_index = self.world.real_nodes().index(edge.start)
        end_index = self.world.real_nodes().index(edge.end)
        from_start_to_depo = 0 if start_index == 0 else self.world.data(start_index, 0).length
        from_end_to_depo = 0 if end_index == 0 else self.world.data(end_index, 0).length
        savings_heuristic = abs(from_start_to_depo + from_end_to_depo - 2 * edge.length + 2 * abs(
            from_start_to_depo - from_end_to_depo)) ** 0.09

        edge_end_index = self.remaining_moves().index(self.world.real_nodes().index(edge.end))
        return post ** self.alpha * pre ** self.beta * self.heuristicF[edge_end_index] * self.heuristicW[
            edge_end_index] * savings_heuristic
