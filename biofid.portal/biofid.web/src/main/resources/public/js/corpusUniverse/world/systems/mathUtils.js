
/**
 * Calculate the euclidean distance of two 3D points
 * @param point1Vector
 * @param point2
 * @returns {number}
 */
function euclideanDistance(point1Vector, point2Vector) {
    const dx = point1Vector.x - point2Vector.x;
    const dy = point1Vector.y - point2Vector.y;
    const dz = point1Vector.z - point2Vector.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

/**
 * DBSCAN for clustering of simple points
 * @param points
 * @param epsilon
 * @param minPoints
 * @returns {{noise: Set<any>, clusters: *[]}}
 */
function dbscan(points, epsilon, minPoints) {
    const clusters = [];
    const visited = new Set();
    const noise = new Set();

    function regionQuery(point) {
        const neighbors = [];
        for (let i = 0; i < points.length; i++) {
            if (euclideanDistance(point, points[i]) < epsilon) {
                neighbors.push(points[i]);
            }
        }
        return neighbors;
    }

    function expandCluster(point, neighbors, cluster) {
        cluster.push(point);
        visited.add(point);

        let i = 0;
        while (i < neighbors.length) {
            const neighbor = neighbors[i];

            if (!visited.has(neighbor)) {
                visited.add(neighbor);
                const neighborNeighbors = regionQuery(neighbor);
                if (neighborNeighbors.length >= minPoints) {
                    neighbors = neighbors.concat(neighborNeighbors);
                }
            }

            if (!clusters.some(cluster => cluster.includes(neighbor))) {
                cluster.push(neighbor);
            }
            i++;
        }
    }

    for (let i = 0; i < points.length; i++) {
        const point = points[i];
        if (!visited.has(point)) {
            visited.add(point);
            const neighbors = regionQuery(point);

            if (neighbors.length < minPoints) {
                noise.add(point);
            } else {
                const cluster = [];
                expandCluster(point, neighbors, cluster);
                clusters.push(cluster);
            }
        }
    }

    return { clusters, noise };
}

/**
 * Given a cluster, this calculates the centroid of it.
 * @param cluster
 * @returns {{x: number, y: number, z: number}}
 */
function calculateClusterCenter(cluster) {
    return calculateCenter(cluster);
}

function calculateCenter(points){
    const center = { x: 0, y: 0, z: 0 };

    points.forEach(point => {
        center.x += point.x;
        center.y += point.y;
        center.z += point.z;
    });

    center.x /= points.length;
    center.y /= points.length;
    center.z /= points.length;

    return center;
}

function addNoise(array, noiseLevel) {
    return array.map(number => {
        const noise = (Math.random() * 2 - 1) * noiseLevel;
        return number + noise;
    });
}

export {euclideanDistance, dbscan, calculateClusterCenter, addNoise, calculateCenter };